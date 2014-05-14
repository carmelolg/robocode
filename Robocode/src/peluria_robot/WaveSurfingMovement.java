package peluria_robot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

/*
 *  Modificare la size bins
 *  Fare statistiche in wave_stats invece di prendere il min tra destra e sinistra
 *   Provare ad aumentare i tick
 */


class EnemyWave {
	Point2D.Double fireLocation;
	long fireTime;
	double bulletVelocity, directAngle, distanceTraveled;
	int direction;

	public EnemyWave() {
	}

}

public class WaveSurfingMovement {

	PeluriaRobot pr;
	public static int STATS_SIZE = 47;
	public static double wave_stats[] = new double[STATS_SIZE];
	public Point2D.Double myLocation; // our bot's location
	public Point2D.Double enemyLocation; // enemy bot's location

	public ArrayList<EnemyWave> enemyWaves;
	public ArrayList<Integer> myDirectionsHistory;
	public ArrayList<Double> absBearingsHistory;
	final int historySize = 5;
	// The index of value in the history that we select
	final int stepIndexAgo = 2;

	public static double opponentEnergy = 100.0;
	public static Rectangle2D.Double battlefieldRect;
	// Minimum distance between Peluria-Bot and Walls
	public static double WALL_STICK = 160;
	// number of max clock to look in the future used to predict the position of
	// Peluria-Bot
	int maxTickToLook = 500;

	public WaveSurfingMovement(PeluriaRobot pr) {
		enemyWaves = new ArrayList<EnemyWave>();
		myDirectionsHistory = new ArrayList<Integer>();
		absBearingsHistory = new ArrayList<Double>();
		this.pr = pr;

	}
	
	public void init(){
		// Create battlefield with size less than real size
		battlefieldRect = new java.awt.geom.Rectangle2D.Double(18, 18, pr.getBattleFieldWidth() - 36, pr.getBattleFieldHeight() - 36);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		// Save my location
		myLocation = new Point2D.Double(pr.getX(), pr.getY());

		// Calculate lateral velocity for know the director of Peluria-Bot
		// if >=0 1(Right) else -1(Left)
		double lateralVelocity = pr.getVelocity() * Math.sin(e.getBearingRadians());
		// Calculate absolute bearing between Peluria-Bot and enemy
		double absBearing = e.getBearingRadians() + pr.getHeadingRadians();

		// Move the Radar in enemy direction
		pr.setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing - pr.getRadarHeadingRadians()) * 2);

		// Add in the history direction and bearing
		myDirectionsHistory.add(0, new Integer((lateralVelocity >= 0) ? 1 : -1));
		absBearingsHistory.add(0, new Double(absBearing + Math.PI));

		// Remove if the histories is > of history size
		if (myDirectionsHistory.size() > historySize)
			myDirectionsHistory.remove(myDirectionsHistory.size() - 1);
		if (absBearingsHistory.size() > historySize)
			absBearingsHistory.remove(absBearingsHistory.size() - 1);

		// Calculate power of bullet of Enemy
		double bulletPower = opponentEnergy - e.getEnergy();
		// If the enemyu shoot and Peluria-Bot have made stepIndexAgo movement
		if (bulletPower < 3.01 && bulletPower > 0.09 && myDirectionsHistory.size() > stepIndexAgo) {
			// Create and add the wave of enemy bullet
			EnemyWave ew = new EnemyWave();
			ew.fireTime = pr.getTime() - 1;
			ew.bulletVelocity = TriUtil.bulletVelocity(bulletPower);
			ew.distanceTraveled = TriUtil.bulletVelocity(bulletPower);
			ew.direction = ((Integer) myDirectionsHistory.get(stepIndexAgo)).intValue();
			ew.directAngle = ((Double) absBearingsHistory.get(stepIndexAgo)).doubleValue();
			ew.fireLocation = (Point2D.Double) enemyLocation.clone();

			enemyWaves.add(ew);
		}

		// Update the energy of the Enemy
		opponentEnergy = e.getEnergy();

		// Perform the location of the enemy
		enemyLocation = TriUtil.project(myLocation, absBearing, e.getDistance());

		updateWaves();
		doSurfing();

	}

	// Update the enemyWaves
	public void updateWaves() {
		for (int x = 0; x < enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave) enemyWaves.get(x);

			// Update the distance traveled by the bullet
			ew.distanceTraveled = (pr.getTime() - ew.fireTime) * ew.bulletVelocity;

			// If the bullet passed Peluria-Bot remove the bullet
			// 50 because we have to remove the bullet that passed Peluria-Bot
			// without hit it
			if (ew.distanceTraveled > myLocation.distance(ew.fireLocation) + 50) {
				enemyWaves.remove(x);
				x--;
			}
		}
	}

	// Get the nearest wave respect to Peluria-Bot
	public EnemyWave getClosestSurfableWave() {
		double closestDistance = Integer.MAX_VALUE;
		EnemyWave surfWave = null;

		for (int x = 0; x < enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave) enemyWaves.get(x);
			double distance = myLocation.distance(ew.fireLocation) - ew.distanceTraveled;

			// We skip the wave that can't avoid (distance <= velocity)
			if (distance > ew.bulletVelocity && distance < closestDistance) {
				surfWave = ew;
				closestDistance = distance;
			}
		}

		return surfWave;
	}

	// Bearing l'angolo tra i due punti. Siccome il robot tende sempre ad essere
	// perpendicolare tende sempre ad avvicinarsi a 90°
	// TargetLocation è la posizione in cui si prevede di essere in quel
	// vettore.
	// Calcola la posizione dove andrai e quindi ritorna un indice per
	// controllare in SerfStats la pericolosità di quel posto.

	// Perform the index of wave_stats in base of the wave and the location
	public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
		// Get the offset of the bearing and the angle of the bullet
		// The bearing is between the fire location and the location
		double offsetAngle = (TriUtil.absoluteBearing(ew.fireLocation, targetLocation) - ew.directAngle);
		// The factor is between -1 and 1 and correspond of and index of
		// wave_stats depends of offset angle
		double factor = Utils.normalRelativeAngle(offsetAngle) / TriUtil.maxEscapeAngle(ew.bulletVelocity) * ew.direction;

		// return the closest factor between 0 and STATS_SIZE-1
		return (int) Math.round(TriUtil.limit(0, (factor * ((STATS_SIZE - 1) / 2)) + ((STATS_SIZE - 1) / 2), STATS_SIZE - 1));
	}

	// Update the value of wave_stats based on the location that Peluria-Bot was
	// hit
	public void logHit(EnemyWave ew, Point2D.Double targetLocation) {
		int index = getFactorIndex(ew, targetLocation);

		for (int x = 0; x < STATS_SIZE; x++) {
			// Increase the value with exponential formula , 1 were hit , 1/3 in
			// the next index , 1/5 ....
			wave_stats[x] += 1.0 / (Math.pow(index - x, 2) + 1);
		}
	}

	public void onHitByBullet(HitByBulletEvent e) {
		// If the enemyWaves collection is empty, we have missed the shoot of
		// enemy
		if (!enemyWaves.isEmpty()) {
			// Get the location where Peluria-Bot was hit
			Point2D.Double hitBulletLocation = new Point2D.Double(e.getBullet().getX(), e.getBullet().getY());
			EnemyWave hitWave = null;

			// look through the EnemyWaves, and find one that could've hit us.
			for (int x = 0; x < enemyWaves.size(); x++) {
				EnemyWave ew = (EnemyWave) enemyWaves.get(x);

				// if the bullet is near to Peluria-Bot and the velocity of the
				// wave is ~ to the bullet velocity this bullet hit us
				if (Math.abs(ew.distanceTraveled - myLocation.distance(ew.fireLocation)) < 50 && Math.abs(TriUtil.bulletVelocity(e.getBullet().getPower()) - ew.bulletVelocity) < 0.001) {
					hitWave = ew;
					break;
				}
			}

			if (hitWave != null) {
				// update the value of wave_stats
				logHit(hitWave, hitBulletLocation);

				// remove this wave
				enemyWaves.remove(enemyWaves.lastIndexOf(hitWave));
			}
		}
	}

	// Predict the location where Peluria-Bot go with the wave and the direction
	public Point2D.Double predictPosition(EnemyWave surfWave, int direction) {
		Point2D.Double predictedPosition = (Point2D.Double) myLocation.clone();
		double predictedVelocity = pr.getVelocity();
		double predictedHeading = pr.getHeadingRadians();
		double maxTurning, moveAngle, moveDir;

		int counter = 0;

		// True if the bullet could hit us
		boolean intercepted = false;

		do {
			// Calculate the angle of movement at counter Tick
			moveAngle = wallSmoothing(predictedPosition, TriUtil.absoluteBearing(surfWave.fireLocation, predictedPosition) + (direction * (Math.PI / 2)), direction) - predictedHeading;
			moveDir = 1;

			if (Math.cos(moveAngle) < 0) {
				moveAngle += Math.PI;
				moveDir = -1;
			}

			moveAngle = Utils.normalRelativeAngle(moveAngle);

			// you can't turn more then this in one tick
			maxTurning = Math.PI / 720d * (40d - 3d * Math.abs(predictedVelocity));
			predictedHeading = Utils.normalRelativeAngle(predictedHeading + TriUtil.limit(-maxTurning, moveAngle, maxTurning));

			// If moveDir > 0 we increment the velocity of 1 else 2
			predictedVelocity += (predictedVelocity * moveDir < 0 ? 2 * moveDir : moveDir);
			predictedVelocity = TriUtil.limit(-8, predictedVelocity, 8);

			// calculate the predicted position
			predictedPosition = TriUtil.project(predictedPosition, predictedHeading, predictedVelocity);

			counter++;

			// If the distance between Peluria-Bot predicted position and the
			// fire location is < of the distance traveled in the count Tick
			// then the bullet could hit
			if (predictedPosition.distance(surfWave.fireLocation) < surfWave.distanceTraveled + (counter * surfWave.bulletVelocity) + surfWave.bulletVelocity) {
				intercepted = true;
			}
		} while (!intercepted && counter < maxTickToLook);

		return predictedPosition;
	}

	// Return the value in the wave_stats based on the wave and the direction
	public double checkDanger(EnemyWave surfWave, int direction) {
		// Calculate the index based on the predicted position that could hit
		// Peluria-Bot
		int index = getFactorIndex(surfWave, predictPosition(surfWave, direction));

		return wave_stats[index];
	}

	// Surfing the wave!!!
	public void doSurfing() {
		EnemyWave surfWave = getClosestSurfableWave();

		if (surfWave == null) {
			return;
		}

		// Calculate the dange if Peluria-Bot go Right or Left
		double dangerLeft = checkDanger(surfWave, -1);
		double dangerRight = checkDanger(surfWave, 1);

		// The angle of Peluria-Bot turn
		double goAngle = TriUtil.absoluteBearing(surfWave.fireLocation, myLocation);

		// Math.PI / 4 because we want to have secure distance to the enemy
		if (dangerLeft < dangerRight) {
			goAngle = wallSmoothing(myLocation, goAngle - (Math.PI / 4), -1);
		} else {
			goAngle = wallSmoothing(myLocation, goAngle + (Math.PI / 4), 1);
		}

		// Move Peluria-Bot
		setBackAsFront(pr, goAngle);
	}

	// Math.PI / 4 . La funzione wallSmoothing calcola la distanza tra
	// myLocation e un punto (in questo caso da dove il colpo è partito)
	// diminuendo il valore da Mat.PI/2 a /4 fa si che ci teniamo più
	// distanti dall'avversario, così da avere più probabilità di evitare i
	// colpi.

	// Project the location and if hit a wall move for avoid the wall changing
	// angle
	public double wallSmoothing(Point2D.Double botLocation, double angle, int orientation) {
		while (!battlefieldRect.contains(TriUtil.project(botLocation, angle, WALL_STICK))) {
			angle += orientation * 0.05;
		}
		return angle;
	}


	// Move the robot in the goAngle direction
	public static void setBackAsFront(AdvancedRobot robot, double goAngle) {
		double angle = Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());
		if (Math.abs(angle) > (Math.PI / 2)) {
			if (angle < 0) {
				robot.setTurnRightRadians(Math.PI + angle);
			} else {
				robot.setTurnLeftRadians(Math.PI - angle);
			}
			robot.setBack(100);
		} else {
			if (angle < 0) {
				robot.setTurnLeftRadians(-1 * angle);
			} else {
				robot.setTurnRightRadians(angle);
			}
			robot.setAhead(100);
		}
	}

	public void onPaint(Graphics2D g) {
		EnemyWave wave = null;
		// try{wave=(EnemyWave)_enemyWaves.get(_enemyWaves.size()-1);}catch(Exception
		// e){};
		try {
			wave = getClosestSurfableWave();
		} catch (Exception e) {
		}
		;

		if (wave == null)
			return;
		int bullet_x = (int) TriUtil.project(wave.fireLocation, wave.directAngle, wave.distanceTraveled).x;
		int bullet_y = (int) TriUtil.project(wave.fireLocation, wave.directAngle, wave.distanceTraveled).y;

		// Draw bullet
		g.setColor(Color.RED);
		g.drawLine((int) wave.fireLocation.x, (int) wave.fireLocation.y, bullet_x, bullet_y);

		// Draw circle
		int ray = (int) wave.fireLocation.distance(bullet_x, bullet_y) * 2;
		g.drawOval((int) wave.fireLocation.x - (ray / 2), (int) wave.fireLocation.y - (ray / 2), ray, ray);

		double max = wave_stats[0];
		double avarage = 0;
		for (int i = 1; i < STATS_SIZE; i++) {
			if (max < wave_stats[i])
				max = wave_stats[i];
			avarage += wave_stats[i];
		}
		avarage /= STATS_SIZE;

		// Draw wave stats
		for (int i = -STATS_SIZE / 2; i < STATS_SIZE / 2; i++) {

			int bx = (int) TriUtil.project(new Point2D.Double(bullet_x, bullet_y), wave.directAngle + Math.PI / 2, i * 10 + 20).x;
			int by = (int) TriUtil.project(new Point2D.Double(bullet_x, bullet_y), wave.directAngle + Math.PI / 2, i * 7 + 20).y;

			g.setColor(new Color((int) (wave_stats[i + STATS_SIZE / 2] / max * 255), 50, 100));
			g.fillRect(bx, by, 5, 20);
		}

	}

}
