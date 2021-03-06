package micycle.pgs.utility;

import static processing.core.PApplet.ceil;
import static processing.core.PApplet.cos;
import static processing.core.PApplet.floor;
import static processing.core.PApplet.max;
import static processing.core.PApplet.min;
import static processing.core.PApplet.sin;
import static processing.core.PApplet.sqrt;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import processing.core.PVector;

/**
 * Computes a sets of random points via Poisson Disk Sampling. Poisson-disc
 * sampling produces points that are tightly-packed, but no closer to each other
 * than a specified minimum distance, resulting in a more natural and desirable
 * pattern for many applications.
 * <p>
 * An implementation of "Fast Poisson Disk Sampling in Arbitrary Dimensions" by
 * Robert Bridson
 * http://www.cs.ubc.ca/~rbridson/docs/bridson-siggraph07-poissondisk.pdf
 * 
 */
public final class PoissonDistribution {

	private ArrayList<ArrayList<PVector>> grid;
	private float cellSize;
	private int gridWidth, gridHeight;
	private float xmin, xmax, ymin, ymax;
	private ArrayList<PVector> points;
	private SplittableRandom random;

	public PoissonDistribution() {
		this(System.currentTimeMillis());
	}

	public PoissonDistribution(final long seed) {
		random = new SplittableRandom(seed);
		points = new ArrayList<>();
	}

	public List<PVector> getPoints() {
		return points;
	}

	public List<PVector> generate(double xmin, double ymin, double xmax, double ymax, double minDist, int rejectionLimit) {
		return generate((float) xmin, (float) ymin, (float) xmax, (float) ymax, (float) minDist, rejectionLimit);
	}

	/**
	 * Generates a poisson point set.
	 * 
	 * @param xmin
	 * @param ymin
	 * @param xmax
	 * @param ymax
	 * @param minDist        minimum distance between points
	 * @param rejectionLimit the limit on the number of attempts to generate a
	 *                       random valid point around the previous point. Generally
	 *                       6 is sufficient.
	 * @return
	 */
	private List<PVector> generate(float xmin, float ymin, float xmax, float ymax, float minDist, int rejectionLimit) {
		this.xmin = xmin;
		this.xmax = xmax;
		this.ymin = ymin;
		this.ymax = ymax;
		final float minDistSquared = minDist * minDist;
		cellSize = (minDist) / sqrt(2); // Rmax/sqrt(d) where d is dimensions
		gridWidth = ceil((xmax - xmin) / cellSize);
		gridHeight = ceil((ymax - ymin) / cellSize);
		int s = gridWidth * gridHeight;
		grid = new ArrayList<>();
		for (int i = 0; i < s; i++) {
			grid.add(new ArrayList<>());
		}

		points.clear();
		final ArrayList<PVector> processList = new ArrayList<>();

		PVector p = new PVector(random(xmin, xmax), random(ymin, ymax)); // ADJACENT SQUARES NEED A
																			// COMMON VALUE
		processList.add(p);
		points.add(p);
		addToGrid(p);

		while (!processList.isEmpty()) {
			final int i = floor(random(0, processList.size())); // Choose a point randomly from the active list, x
			p = processList.remove(i);

			/*
			 * Create up to k new points uniformly at random in the spherical annulus
			 * between radii r and 2r centered on xi
			 */
			for (int k = 0; k < rejectionLimit; k++) {
				PVector n = createRandomPointAround(p, minDist, minDist * 2);
				if (insideBoundaries(n) && testGrid(n, minDist, minDistSquared)) {
					processList.add(n);
					points.add(n);
					addToGrid(n);
				}
			}
		}

		return new ArrayList<>(points);
	}

	private boolean insideBoundaries(PVector p) {
		return (p.x >= xmin && p.x < xmax && p.y >= ymin && p.y < ymax); // keep center points in bounds
	}

	/**
	 * Create points randomly on a spherical annulus.
	 * 
	 * @param p
	 * @param minDist inner radius
	 * @param maxDist outer radius
	 **/
	private PVector createRandomPointAround(PVector p, float minDist, float maxDist) {
		final float a = random(0, (2 * Math.PI));
		final float r = random(minDist, maxDist);
		return new PVector(p.x + r * cos(a), p.y + r * sin(a));
	}

	/**
	 * 
	 * @param p
	 * @param minDist
	 * @return true if there are no points inside the circle of minDist radius
	 *         around p
	 */
	private boolean testGrid(PVector p, float minDist, float minDistSquared) {
		int minX = floor(max(0, (p.x - minDist - xmin) / cellSize));
		int maxX = ceil(min(gridWidth - 1f, (p.x + minDist - xmin) / cellSize));
		int minY = floor(max(0, (p.y - minDist - ymin) / cellSize));
		int maxY = ceil(min(gridHeight - 1f, (p.y + minDist - ymin) / cellSize));

		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				ArrayList<PVector> cell = grid.get(y * gridWidth + x);
				for (PVector t : cell) {
					float dx = p.x - t.x;
					float dy = p.y - t.y;
					if (dx * dx + dy * dy <= minDistSquared) {
						return false;
					}
				}
			}
		}

		return true;
	}

	private void addToGrid(PVector p) {
		grid.get(index(p.x, p.y)).add(p);
	}

	private int index(float x, float y) {
		int gx = floor((x - xmin) / cellSize);
		int gy = floor((y - ymin) / cellSize);
		return gy * gridWidth + gx;
	}

	/**
	 * @param min - The minimum.
	 * @param max - The maximum.
	 * @return A random double between these numbers (inclusive the minimum and
	 *         maximum).
	 */
	private float random(double min, double max) {
		return (float) (min + (max - min) * random.nextDouble());
	}

}