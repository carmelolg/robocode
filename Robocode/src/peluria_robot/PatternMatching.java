package peluria_robot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;

import robocode.HitByBulletEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;


class EnemyMovement {
	// Enemy velocity
	double velocity;
	// Enemy heading
	double heading;
	// Enemy location
	Point2D.Double location;

	// Delta = difference between one step ago and now
	// Delta velocity
	double dVelocity;
	// Delta heading
	double dHeading;
	// Delta distance
	double dDistance;

	public EnemyMovement(double velocity, double heading, Double location,
			double dVelocity, double dHeading, double dDistance) {
		this.velocity = velocity;
		this.heading = heading;
		this.location = location;
		this.dVelocity = dVelocity;
		this.dHeading = dHeading;
		this.dDistance = dDistance;
	}

	// Compare the movement with mov
	double compare(EnemyMovement mov) {
		//Normalize the variable
		double maxVelocity = 8.0;
		double maxHeading = (10 - 0.75 * Math.abs(velocity));
		double maxDistance = velocity * PatternMatching.TICK_SCAN;

		//Calculate the Euclidean distance
		
		double distanceVelocity = Math.pow(Math.abs(dVelocity / maxVelocity)
				- Math.abs(mov.dVelocity / maxVelocity), 2);
		double distanceHeading = Math.pow(dHeading / maxHeading - mov.dHeading
				/ maxHeading, 2);
		double distanceDistance = Math.pow(dDistance / maxDistance
				- mov.dDistance / maxDistance, 2);


		return Math.sqrt(distanceVelocity + distanceHeading + distanceDistance);

	}

}

public class PatternMatching {

	static ArrayList<EnemyMovement> logEnemy = new ArrayList<EnemyMovement>();
	PeluriaRobot pr;

	public PatternMatching(PeluriaRobot pr) {
		this.pr = pr;
	}

	// Tick elapsed between each scan
	final static int TICK_SCAN = 3;
	// THRESHOLD for the pattern evaluation that if is < not fire
	static final double FIRING_THRESHOLD = 0.3;

	// Last movement to search in the log movement
	final int LAST_MOVEMENT_SIZE = 10;

	Point2D.Double enemyLocation;
	Point2D.Double enemyFutureLocation;

	public boolean noGuessFactorIJustFire(ScannedRobotEvent e, double the_power) {
		// In melee not use pattern matching
		if(pr.getOthers() > 1)
			return false;
		
		// Bearing betwen Peluria-Bot and enemy
		double absBearing = pr.getHeadingRadians() + e.getBearingRadians();
		// Enemy Location
		enemyLocation = TriUtil.project(
				new Point2D.Double(pr.getX(), pr.getY()), absBearing,
				e.getDistance());

		// Add movement in log
		addLogMovement(e, enemyLocation);

		int bestPattern = 0;
		double bestEvaluation = Integer.MAX_VALUE;
		
		// Peluria-Bot search in the log the movement pattern until the pattern in the log
		// If the log is small stop not fire
		if (logEnemy.size() - 2 * LAST_MOVEMENT_SIZE + 1 <= 0)
			return false;
		
		//Evaluate the pattern movement of enemy
		for (int i = 0; i < logEnemy.size() - 2 * LAST_MOVEMENT_SIZE + 1; i++) {
			//Evaluate the  i movement
			double evaluationPattern = evaluate(i);

			if (evaluationPattern < bestEvaluation) {
				bestEvaluation = evaluationPattern;
				bestPattern = i;
			}

		}

		if(bestEvaluation > FIRING_THRESHOLD)
			return false;
		
		// Pick the future movement after the pattern
		bestPattern += LAST_MOVEMENT_SIZE;
		
		double power = the_power;
		double powerVel = 20 - 3 * power;

		double timeImpact = e.getDistance() / powerVel;

		// Estimate the future position of enemy based on the velocity of bullet
		int indexFutureMovement = (int) (timeImpact / TICK_SCAN);

		// If the index exceeded the log not fire
		if (bestPattern + indexFutureMovement > logEnemy.size())
			return false;

		// Calculate the future location of enemy based on the log movement
		double angleFuture = 0;
		double futureDistance = 0;
		for (int i = bestPattern; i < bestPattern + indexFutureMovement; i++) {
			angleFuture += logEnemy.get(i).dHeading;
			futureDistance += logEnemy.get(i).dDistance;
		}
		enemyFutureLocation = TriUtil.project(enemyLocation, angleFuture,
				futureDistance);
		
		double absFutureBearing = TriUtil.absoluteBearing(pr.getX(), pr.getY(),
				enemyFutureLocation.x, enemyFutureLocation.y);

		double gunAdjust = Utils.normalRelativeAngle(absFutureBearing
				- pr.getGunHeadingRadians());

		pr.setTurnGunRightRadians(gunAdjust);

		if (pr.getGunHeat() == 0 && Math.abs(pr.getGunTurnRemaining()) < GuessFactorTargeting.TURN_REMAINING)
			pr.setFireBullet(power);
		System.out.println("PATTERN MATCHING");
		return true;
	}

	// Evaluate the pattern movement starting from i
	private double evaluate(int i) {

		double evaluation = 0;
		for (int j = i; j < LAST_MOVEMENT_SIZE + i; j++) {

			// Movement done in the past
			EnemyMovement movementToEvaluate = logEnemy.get(j);
			// Movement done in the current time
			EnemyMovement currentEvaluate = logEnemy.get(logEnemy.size()
					- LAST_MOVEMENT_SIZE + (j - i));
			//Sum the distance between the movement
			evaluation += currentEvaluate.compare(movementToEvaluate);
		}

		return evaluation;
	}

	public void addLogMovement(ScannedRobotEvent e, Point2D.Double enemyLocation) {
		// Add movement each TICK_SCAN
		if (e.getTime() % TICK_SCAN != 0)
			return;

		double deltaVelocity = 0;
		double deltaHeading = 0;
		double deltaDistance = 0;

		if (logEnemy.size() > 0) {
			EnemyMovement lastMovement = logEnemy.get(logEnemy.size() - 1);
			//Calculate the delta velocity,heading and distance
			deltaVelocity = Math.abs(lastMovement.velocity - e.getVelocity());
			deltaHeading = Math.abs(lastMovement.heading
					- e.getHeadingRadians());
			deltaDistance = Math.abs(lastMovement.location
					.distance(enemyLocation));
		}

		EnemyMovement movement = new EnemyMovement(e.getVelocity(),
				e.getHeadingRadians(), enemyLocation, deltaVelocity,
				deltaHeading, deltaDistance);

		logEnemy.add(movement);
	}

	public void onRobotDeath(RobotDeathEvent e) {

	}

	public void onPaint(Graphics2D g) {
		if (logEnemy.size() < LAST_MOVEMENT_SIZE)
			return;

		g.setColor(Color.RED);

		g.drawRect((int) enemyFutureLocation.x, (int) enemyFutureLocation.y,
				10, 10);

		Point2D.Double location = enemyLocation;
		for (int i = 0; i < LAST_MOVEMENT_SIZE; i++) {
			EnemyMovement movement = logEnemy.get(logEnemy.size() - 1 - i);
			location = TriUtil.project(location, movement.dHeading,
					movement.dDistance);

			g.setColor(Color.GREEN);

			g.drawRect((int) movement.location.x, (int) movement.location.y,
					10, 10);
		}

	}

	public void onHitByBullet(HitByBulletEvent event) {

	}

}
