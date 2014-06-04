package peluria_robot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import robocode.ScannedRobotEvent;
import robocode.util.Utils;

class WaveBullet {
	// Position of the fire location, angle and power of the bullet
	public double startX, startY, bearing, power;
	// Time when the bullet is fired
	public long fireTime;
	// Direction of bullet based of the enemy movement
	public int direction;
	// Guess Factor of the targeting
	private double[] stats;
	// Distance of the enemy when fire
	public double enemyDistance;
	// GuessFactor used when fire bullet
	public double GF;

	public WaveBullet(double x, double y, double bearing, double power, int direction, long time, double[] stats) {
		this.startX = x;
		this.startY = y;
		this.bearing = bearing;
		this.power = power;
		this.direction = direction;
		this.fireTime = time;
		this.stats = stats;
	}

	public double maxEscapeAngle() {
		return TriUtil.maxEscapeAngle(TriUtil.bulletVelocity(power));
	}

	// Return true if the wave hit enemy , false otherwise
	public boolean checkHit(double enemyX, double enemyY, long currentTime) {
		// If the distance of the fire location and the enemy location is <= of
		// the distance traveled of the
		// bullet then the bullet could hit the enemy
		if (Point2D.distance(startX, startY, enemyX, enemyY) <= (currentTime - fireTime) * TriUtil.bulletVelocity(power)) {

			// The bearing between fire location and the enemy location
			double desiredAngle = TriUtil.absoluteBearing(startX, startY, enemyX, enemyY);
			// The angle offset between the angle of bullet fired and the angle
			// of enemy
			double angleOffset = Utils.normalRelativeAngle(desiredAngle - bearing);
			// Calculate the guess factor between -1 and 1 , 0 if we hit the
			// enemy.
			double guessFactor = TriUtil.limit(-1, angleOffset / maxEscapeAngle(), 1) * direction;

			// Betwen 0 and stats.length-1. The index for improvement the
			// accuracy of the future firing
			int index = (int) Math.round((stats.length - 1) / 2 * (guessFactor + 1));
			for (int x = 0; x < stats.length; x++) {
				// Increase the value with exponential formula , 1 were hit , 1/2 , 1/5, 1/7 ...
				stats[x] += 1.0 / (Math.pow(index - x, 2) + 1);
			}

			return true;
		}
		return false;
	}
}

public class GuessFactorTargeting {
	// List of waves
	List<WaveBullet> waves = new ArrayList<WaveBullet>();
	// Peluria-Bot
	PeluriaRobot pr;
	PatternMatching pm;

	// Degree remaining to turn the cannon
	final static int TURN_REMAINING = 10;

	// The vector of stats of the bullet fired
	final static int STATS_SIZE = 105;
	final static int SEG_DISTANCE_SIZE = 5;
	final static int SEG_VELOCITY_SIZE = 3;
	static double[][][] stats = new double[SEG_DISTANCE_SIZE][SEG_VELOCITY_SIZE][STATS_SIZE];

	// Location of Peluria-Bot and enemy
	Point2D.Double myLocation = new Point2D.Double();
	Point2D.Double enemyLocation = new Point2D.Double();

	// Threshold for the bullet power
	double energyThreshold = 10;

	// Current enemy direction
	int direction = 1;

	public GuessFactorTargeting(PeluriaRobot pr) {
		this.pr = pr;
		pm = new PatternMatching(pr);
	}

	public void onScannedRobot(ScannedRobotEvent e) {

		// Perform the power of bullet
		double power = getBulletPower(e.getDistance());

		

		// Bearing betwen Peluria-Bot and enemy
		double absBearing = pr.getHeadingRadians() + e.getBearingRadians();

		// Update Peluria-Bot location
		myLocation.x = pr.getX();
		myLocation.y = pr.getY();

		// Enemy Location
		enemyLocation = TriUtil.project(myLocation, absBearing, e.getDistance());
		double ex = enemyLocation.x;
		double ey = enemyLocation.y;

		// Update the waves
		for (int i = 0; i < waves.size(); i++) {
			WaveBullet currentWave = (WaveBullet) waves.get(i);
			if (currentWave.checkHit(ex, ey, pr.getTime())) {
				waves.remove(currentWave);
				i--;
			}
		}

		// If Pattern Matching targeting fire stop Guess Factor
		if (pm.noGuessFactorIJustFire(e, power)) 
			return;

		// Perform the direction of enemy , if enemy don't move take the
		// previous direction
		if (e.getVelocity() != 0) {
			if (Math.sin(e.getHeadingRadians() - absBearing) * e.getVelocity() < 0)
				direction = -1;
			else
				direction = 1;
		}

		double segmentedStats[] = getSegmentatedStats(e.getDistance(), e.getVelocity());

		// Create the wave
		WaveBullet newWave = new WaveBullet(myLocation.x, myLocation.y, absBearing, power, direction, pr.getTime(), segmentedStats);
		newWave.enemyDistance = e.getDistance();

		// Find the best index in the stats
		
		int bestindex = (STATS_SIZE - 1) / 2;
		// If the enemy don't move shoot in with no guess factor
		if (e.getEnergy() == 0) {
			bestindex = (STATS_SIZE - 1) / 2;
		} else {
			// Calculate the guess factor, start to head on enemy
			for (int i = 0; i < STATS_SIZE; i++)
				if (segmentedStats[bestindex] < segmentedStats[i])
					bestindex = i;
		}
		
		// Perform the Guess Factor angle from the best index
		double guessfactor = (double) (bestindex - (STATS_SIZE - 1) / 2) / ((STATS_SIZE - 1) / 2);
		newWave.GF = guessfactor;

		// Perform the angle of shoot based on the guess factor and the bearing
		// with the enemy
		double angleOffset = direction * guessfactor * newWave.maxEscapeAngle();
		double gunAdjust = Utils.normalRelativeAngle(absBearing - pr.getGunHeadingRadians() + angleOffset);

		pr.setTurnGunRightRadians(gunAdjust);

		// If Peluria-Bot cannon is cold and gun doesn't have to turn anymore or
		// by just a very small amount.
		if (pr.getGunHeat() == 0 && Math.abs(pr.getGunTurnRemaining()) < TURN_REMAINING && pr.setFireBullet(power) != null)
			waves.add(newWave);
	}

	// Return segmented stats
	private double[] getSegmentatedStats(double distance, double velocity) {
		int indexDistance = (int) Math.round((distance / getMaxDIstance() * (SEG_DISTANCE_SIZE - 1)));
		int indexVelocity = (int) Math.round((Math.abs(velocity) / 8.0 * (SEG_VELOCITY_SIZE - 1)));

		return stats[indexDistance][indexVelocity];
	}


	// Power is between 1 and 3 and perform the power based on the distance between the enemy and my location
	double getBulletPower(double distance) {
		double maxPower = 3;
		if (pr.getEnergy() < energyThreshold)
			maxPower = 0.2;

		return TriUtil.limit(1.0, (1 - (distance / Math.sqrt(Math.pow(pr.getBattleFieldHeight(), 2) + Math.pow(pr.getBattleFieldWidth(), 2)))) * maxPower, 3);
	}

	double getMaxDIstance() {
		return Math.sqrt(Math.pow(pr.getBattleFieldHeight(), 2) + Math.pow(pr.getBattleFieldWidth(), 2));
	}

	public void onPaint(Graphics2D g) {
		WaveBullet wave = null;
		try {
			wave = waves.get(waves.size() - 1);
		} catch (Exception e) {
			return;
		}
		;

		g.setColor(Color.GREEN);
		int enemyX = (int) TriUtil.project(new Point2D.Double(wave.startX, wave.startY), wave.bearing, wave.enemyDistance).x;
		int enemyY = (int) TriUtil.project(new Point2D.Double(wave.startX, wave.startY), wave.bearing, wave.enemyDistance).y;

		int enemyMEAX = (int) TriUtil.project(new Point2D.Double(wave.startX, wave.startY), wave.bearing + wave.maxEscapeAngle() * wave.direction, wave.enemyDistance).x;
		int enemyMEAY = (int) TriUtil.project(new Point2D.Double(wave.startX, wave.startY), wave.bearing + wave.maxEscapeAngle() * wave.direction, wave.enemyDistance).y;

		g.drawLine(enemyX, enemyY, enemyMEAX, enemyMEAY);

		int bulletX = (int) TriUtil.project(new Point2D.Double(wave.startX, wave.startY), wave.bearing + wave.maxEscapeAngle() * wave.direction * wave.GF, wave.enemyDistance).x;
		int bulletY = (int) TriUtil.project(new Point2D.Double(wave.startX, wave.startY), wave.bearing + wave.maxEscapeAngle() * wave.direction * wave.GF, wave.enemyDistance).y;
		g.fillRect(bulletX, bulletY, 10, 10);

	}
}