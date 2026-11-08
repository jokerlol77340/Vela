package app.vela.core.nav

import app.vela.core.model.LatLng
import app.vela.core.model.TravelMode

/**
 * Where to try a side street around the plate cameras on a route (issue #600).
 *
 * The reporter's own workaround, automated: for each cluster of cameras the leading route passes,
 * try a point a little way to either side of the road at the cluster and route THROUGH it. The
 * router's own snap does the graph work: a point that lands on the same road folds back into the
 * same route (the camera count does not drop, so the caller rejects it), a point that lands on a
 * parallel street is a real detour. Nothing here knows about roads; it only says where to ask.
 *
 * Bounded on purpose: at most [MAX_CLUSTERS] clusters per route, nearest first, at most
 * [MAX_REQUESTS_PER_ROUTE] route requests for one candidate route and [MAX_REQUESTS] for the whole
 * trip, because each one is a full route request to a fair-use community router plus Google.
 * The pass runs over every candidate route that passes cameras, not only the one that leads
 * after the re-rank: the two disagree when a route's cameras sit on an arterial with a parallel
 * street while the leader's one camera sits on a bridge. Routes share arterials, so a cluster
 * already tried on another route is skipped ([untried]); that is what keeps the trip budget at
 * six requests instead of six per route.
 */
object CameraDetour {

    /** How far off the road a candidate point sits. Far enough to reach the next street over in a
     *  city block, near enough that a rural road with nothing beside it snaps straight back. */
    const val OFFSET_M = 150.0
    const val MAX_CLUSTERS = 3
    const val MAX_REQUESTS = 6
    const val MAX_REQUESTS_PER_ROUTE = 4
    /** A cluster within this distance of one already tried on another route is the same corner. */
    const val SAME_CLUSTER_M = 60.0

    /** A camera cluster on the route ([atM] along it, [count] heads, [at] its point) and the two
     *  points to try. */
    data class Candidate(val atM: Double, val count: Int, val left: LatLng, val right: LatLng, val at: LatLng)

    /** A camera cluster with multiple lateral offsets (progressive radii) for Total Avoidance. */
    data class CandidateMulti(val atM: Double, val count: Int, val vias: List<LatLng>, val at: LatLng)

    /** A route as the camera choice sees it: how many cameras it passes and its shown time. */
    data class Option(val cameras: Int, val etaS: Double)

    /**
     * The one rule for leading with a camera-lighter route, shared by the re-rank over the routers'
     * own alternates and the pick over the side-street results: the index of the option with the
     * fewest cameras (ties to the faster) when it passes fewer cameras than option 0 and costs at
     * most [cap] seconds over [eta0]; null when option 0 stays. Option 0 is the route that leads
     * now, [eta0] the fastest route's time (the cap is measured from the fastest, not the leader).
     */
    fun choose(options: List<Option>, eta0: Double, cap: Double): Int? {
        if (options.isEmpty()) return null
        val best = options.indices.minByOrNull { options[it].cameras * 1_000_000L + options[it].etaS.toLong() } ?: return null
        if (best == 0) return null
        val o = options[best]
        return if (o.cameras < options[0].cameras && o.etaS - eta0 <= cap) best else null
    }

    /** [cands] minus every cluster within [withinM] of a point in [tried]: the same corner reached
     *  from another route gets the same side streets, so asking again spends requests for nothing. */
    fun untried(cands: List<Candidate>, tried: List<LatLng>, withinM: Double = SAME_CLUSTER_M): List<Candidate> =
        cands.filter { c -> tried.none { t -> distM(c.at, t) <= withinM } }

    fun untriedMulti(cands: List<CandidateMulti>, tried: List<LatLng>, withinM: Double = SAME_CLUSTER_M): List<CandidateMulti> =
        cands.filter { c -> tried.none { t -> distM(c.at, t) <= withinM } }

    private fun distM(a: LatLng, b: LatLng): Double {
        val k = 111_320.0 * Math.cos(Math.toRadians(a.lat))
        return Math.hypot((a.lng - b.lng) * k, (a.lat - b.lat) * 111_320.0)
    }

    /** Candidates for the clusters of [cameraAlongM] (distances along [poly], any order), nearest
     *  first, at most [maxClusters]. Left is the driver's left at the route's bearing there. */
    fun candidates(
        poly: List<LatLng>,
        cameraAlongM: List<Double>,
        offsetM: Double = OFFSET_M,
        maxClusters: Int = MAX_CLUSTERS,
    ): List<Candidate> {
        if (poly.size < 2 || cameraAlongM.isEmpty()) return emptyList()
        val cum = RouteProjection.cumulative(poly)
        return CameraAlerts.group(cameraAlongM.sorted()).take(maxClusters).map { g ->
            val p = RouteProjection.pointAt(poly, cum, g.atM)
            val brg = RouteProjection.bearingAt(poly, cum, g.atM)
            Candidate(g.atM, g.count, offset(p, brg - 90.0, offsetM), offset(p, brg + 90.0, offsetM), p)
        }
    }

    /**
     * Candidates with multiple progressive offset distances adapted by travel mode
     * and pre-camera exit anticipation for Total Avoidance.
     */
    fun candidatesMulti(
        poly: List<LatLng>,
        cameraAlongM: List<Double>,
        isTotalAvoidance: Boolean,
        mode: TravelMode = TravelMode.DRIVE,
        maxClusters: Int = if (isTotalAvoidance) 8 else MAX_CLUSTERS,
    ): List<CandidateMulti> {
        if (poly.size < 2 || cameraAlongM.isEmpty()) return emptyList()
        val cum = RouteProjection.cumulative(poly)
        val (offsets, preExitM) = when (mode) {
            TravelMode.WALK -> {
                if (isTotalAvoidance) {
                    listOf(50.0, 100.0, 200.0, 350.0) to 30.0
                } else {
                    listOf(50.0, 100.0) to 30.0
                }
            }
            TravelMode.BICYCLE -> {
                if (isTotalAvoidance) {
                    listOf(100.0, 200.0, 450.0, 800.0) to 60.0
                } else {
                    listOf(100.0, 200.0) to 60.0
                }
            }
            else -> {
                if (isTotalAvoidance) {
                    listOf(250.0, 500.0, 1000.0, 1800.0) to 150.0
                } else {
                    listOf(OFFSET_M, 300.0) to 150.0
                }
            }
        }
        return CameraAlerts.group(cameraAlongM.sorted()).take(maxClusters).map { g ->
            val p = RouteProjection.pointAt(poly, cum, g.atM)
            val brg = RouteProjection.bearingAt(poly, cum, g.atM)
            val vias = ArrayList<LatLng>()
            for (off in offsets) {
                vias.add(offset(p, brg - 90.0, off))
                vias.add(offset(p, brg + 90.0, off))
                if (isTotalAvoidance && off >= offsets[1] && g.atM >= preExitM) {
                    val pBefore = RouteProjection.pointAt(poly, cum, (g.atM - preExitM).coerceAtLeast(0.0))
                    val brgBefore = RouteProjection.bearingAt(poly, cum, (g.atM - preExitM).coerceAtLeast(0.0))
                    vias.add(offset(pBefore, brgBefore - 90.0, off))
                    vias.add(offset(pBefore, brgBefore + 90.0, off))
                }
            }
            CandidateMulti(g.atM, g.count, vias, p)
        }
    }

    /**
     * Wide detour candidates designed specifically for Total Avoidance.
     * Looks far enough to find real, logical alternative corridors (parallel avenues,
     * bypasses, grand contournements) that completely circumvent camera corridors.
     */
    fun totalAvoidanceCandidates(
        origin: LatLng,
        dest: LatLng,
        poly: List<LatLng>,
        cameraAlongM: List<Double>,
        mode: TravelMode = TravelMode.DRIVE,
    ): List<CandidateMulti> {
        if (poly.size < 2 || cameraAlongM.isEmpty()) return emptyList()
        val cum = RouteProjection.cumulative(poly)
        val totalM = cum.lastOrNull() ?: return emptyList()
        val minM = cameraAlongM.minOrNull() ?: 0.0
        val maxM = cameraAlongM.maxOrNull() ?: 0.0
        val corridorMidM = ((minM + maxM) / 2.0).coerceIn(0.0, totalM)
        val pCorridorMid = RouteProjection.pointAt(poly, cum, corridorMidM)
        val brgCorridorMid = RouteProjection.bearingAt(poly, cum, corridorMidM)

        val tripMid = LatLng((origin.lat + dest.lat) / 2.0, (origin.lng + dest.lng) / 2.0)
        val tripBrg = bearing(origin, dest)
        val tripDist = distM(origin, dest)

        val preExitM = (minM - 150.0).coerceAtLeast(0.0)
        val pPreExit = RouteProjection.pointAt(poly, cum, preExitM)
        val brgPreExit = RouteProjection.bearingAt(poly, cum, preExitM)

        val (corridorOffsets, tripOffsets) = when (mode) {
            TravelMode.WALK -> {
                listOf(120.0, 250.0, 450.0) to listOf(150.0, 300.0, 500.0)
            }
            TravelMode.BICYCLE -> {
                listOf(200.0, 400.0, 750.0, 1200.0) to listOf(250.0, 500.0, 900.0)
            }
            else -> {
                // DRIVE: look far enough! 350m to 2800m spans suburban & urban parallel networks
                listOf(350.0, 600.0, 950.0, 1400.0, 2000.0, 2800.0) to listOf(
                    (tripDist * 0.25).coerceIn(400.0, 1500.0),
                    (tripDist * 0.45).coerceIn(700.0, 2500.0),
                    (tripDist * 0.70).coerceIn(1100.0, 3500.0),
                    (tripDist * 1.00).coerceIn(1600.0, 5000.0),
                ).distinct()
            }
        }

        val prioritizedVias = ArrayList<LatLng>()

        // 1. Interleave corridor midpoint and trip midpoint bypasses with progressive widths
        val maxPairs = maxOf(corridorOffsets.size, tripOffsets.size)
        for (i in 0 until maxPairs) {
            if (i < corridorOffsets.size) {
                val off = corridorOffsets[i]
                prioritizedVias.add(offset(pCorridorMid, brgCorridorMid - 90.0, off))
                prioritizedVias.add(offset(pCorridorMid, brgCorridorMid + 90.0, off))
            }
            if (i < tripOffsets.size) {
                val off = tripOffsets[i]
                prioritizedVias.add(offset(tripMid, tripBrg - 90.0, off))
                prioritizedVias.add(offset(tripMid, tripBrg + 90.0, off))
            }
            if (i == 1 && preExitM > 50.0) {
                val off = corridorOffsets[minOf(1, corridorOffsets.lastIndex)]
                prioritizedVias.add(offset(pPreExit, brgPreExit - 90.0, off))
                prioritizedVias.add(offset(pPreExit, brgPreExit + 90.0, off))
            }
        }

        val wideCandidate = CandidateMulti(corridorMidM, cameraAlongM.size, prioritizedVias.distinct(), pCorridorMid)
        val clusterCandidates = candidatesMulti(poly, cameraAlongM, isTotalAvoidance = true, mode = mode, maxClusters = 4)

        return listOf(wideCandidate) + clusterCandidates
    }

    /** Direction from [a] to [b] in degrees (0..360, 0 = North, 90 = East). */
    fun bearing(a: LatLng, b: LatLng): Double {
        val dx = (b.lng - a.lng) * kotlin.math.cos(Math.toRadians(a.lat))
        val dy = b.lat - a.lat
        return (Math.toDegrees(kotlin.math.atan2(dx, dy)) + 360.0) % 360.0
    }

    /** [p] moved [meters] along compass [bearingDeg]. Planar; exact to centimeters at this range. */
    internal fun offset(p: LatLng, bearingDeg: Double, meters: Double): LatLng {
        val b = Math.toRadians(bearingDeg)
        val dLat = meters * Math.cos(b) / 111_320.0
        val dLng = meters * Math.sin(b) / (111_320.0 * Math.cos(Math.toRadians(p.lat)))
        return LatLng(p.lat + dLat, p.lng + dLng)
    }

    /** The user's stops and the detour points merged into ONE travel-ordered list by their
     *  position along the route. A stop with no position (not on this line) keeps its place by
     *  index between the ones that have one. */
    fun mergePlan(stops: List<Pair<Double?, LatLng>>, vias: List<Pair<Double, LatLng>>): List<LatLng> =
        mergeOrdered(stops, vias)

    /** [mergePlan] over anything: stops in list order with a position where known, vias with a
     *  position, out as one list ordered along the route. A stop with no position is placed
     *  halfway to the next positioned stop, else just past the previous one. Used by the nav
     *  session too, to keep the silent detour vias in place when the visible stops are edited. */
    fun <T> mergeOrdered(stops: List<Pair<Double?, T>>, vias: List<Pair<Double, T>>): List<T> {
        val filled = ArrayList<Pair<Double, T>>(stops.size)
        var last = 0.0
        for ((i, s) in stops.withIndex()) {
            val at = s.first ?: (stops.drop(i + 1).firstNotNullOfOrNull { it.first }?.let { (last + it) / 2 } ?: (last + 1.0))
            filled += at to s.second
            last = at
        }
        return (filled + vias).sortedBy { it.first }.map { it.second }
    }
}
