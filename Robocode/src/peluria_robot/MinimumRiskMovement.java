package peluria_robot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import robocode.HitByBulletEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;


public class MinimumRiskMovement {

	HashMap<String, EnemyInfo> enemies = new HashMap<String, EnemyInfo>();
	EnemyInfo target;
	Point2D.Double nextLocation;
	Point2D.Double lastLocation;
	Point2D.Double myLocation;
	double myEnergy;

	final double DISTANCE_NEXT_OFFSET = 15;
	final double POINT_GEN = 1000;
	final double MIN_DISTANCE_TARGET = 0.8;
	final double MIN_DISTANCE_MYLOCATION = 100;
	final double DISTANCE_MYLOCATION = 50;

	static Rectangle2D.Double battleField;

	PeluriaRobot pr;

	public MinimumRiskMovement(PeluriaRobot pr) {
		this.pr = pr;

	}

	public void init() {
		myLocation = new Point2D.Double(pr.getX(), pr.getY());
		target = new EnemyInfo();
		nextLocation = myLocation;
		lastLocation = myLocation;
		battleField = new Rectangle2D.Double(18, 18, pr.getBattleFieldWidth() - 36, pr.getBattleFieldHeight() - 36);
		pr.setColors(Color.GRAY, Color.RED, Color.LIGHT_GRAY);
		pr.setScanColor(Color.LIGHT_GRAY);

	}


	public void run() {

		myLocation = new Point2D.Double(pr.getX(), pr.getY());
		myEnergy = pr.getEnergy();
		
		
		
		pr.setTurnRadarRightRadians(Double.POSITIVE_INFINITY  );
		

		if (target.live) {
			doMovementAndGun();
		}
	}

	private void doMovementAndGun() {

		double distanceToNextDestination = myLocation.distance(nextLocation);

		// search a new destination if I reached this one
		if (distanceToNextDestination < DISTANCE_NEXT_OFFSET) {

			nextLocation = getNextPoint();

			lastLocation = myLocation;

		} else {

			// just the normal goTo stuff
			double angle = TriUtil.absoluteBearing(myLocation, nextLocation) - pr.getHeadingRadians();
			double direction = 1;

			if (Math.cos(angle) < 0) {
				angle += Math.PI;
				direction = -1;
			}

			pr.setAhead(distanceToNextDestination * direction);
			pr.setTurnRightRadians(angle = Utils.normalRelativeAngle(angle));
			// hitting walls isn't a good idea, but HawkOnFire still does it
			// pretty often
			pr.setMaxVelocity(Math.abs(angle) > 1 ? 0 : 8d);

		}

	}

	public Point2D.Double getNextPoint() {
		Point2D.Double testPoint;
		Point2D.Double bestPoint = null;
		double distanceToTarget = myLocation.distance(target.location);
		int i = 0;

		do {
			// calculate the testPoint somewhere around the current position.
			// 100 + 200*Math.random() proved to be good if there are
			// around 10 bots in a 1000x1000 field. but this needs to be limited
			// this to distanceToTarget*0.8. this way the bot wont
			// run into the target (should mostly be the closest bot)
			testPoint = generatePoint(distanceToTarget);
			if (battleField.contains(testPoint) && (bestPoint == null || riskEvaluation(testPoint) < riskEvaluation(bestPoint))) {
				bestPoint = testPoint;
			}
		} while (i++ < POINT_GEN);

		return bestPoint;
	}

	public Point2D.Double generatePoint(double distanceToTarget) {
		return TriUtil.project(myLocation, 2 * Math.PI * Math.random(), Math.min(distanceToTarget * MIN_DISTANCE_TARGET, MIN_DISTANCE_MYLOCATION + DISTANCE_MYLOCATION * Math.random()));
	}

	public double riskEvaluation(Point2D.Double point) {

		// Coulomb's law , The force of attraction is inversely proportional to
		// the square of the distance
		double eval = 1 / point.distanceSq(lastLocation);

		for (String key : enemies.keySet()) {
			EnemyInfo en = enemies.get(key);
			// this is the heart of HawkOnFire. So I try to explain what I
			// wanted to do:
			// - Math.min(en.energy/myEnergy,2) is multiplied because
			// en.energy/myEnergy is an indicator how dangerous an enemy is
			// - Math.abs(Math.cos(calcAngle(myPos, p) - calcAngle(en.pos, p)))
			// is bigger if the moving direction isn't good in relation
			// to a certain bot. it would be more natural to use
			// Math.abs(Math.cos(calcAngle(p, myPos) - calcAngle(en.pos,
			// myPos)))
			// but this wasn't going to give me good results
			// - 1 / p.distanceSq(en.pos) is just the normal anti gravity thing
			if (en.live) {
				eval += Math.min(en.energy / myEnergy, 2) * (1 + Math.abs(Math.cos(TriUtil.absoluteBearing(point, myLocation) - TriUtil.absoluteBearing(point, en.location)))) *
						en.damage / (point.distanceSq(en.location) *0.1);
			}
		}
		return eval;
	}

	public void onScannedRobot(ScannedRobotEvent e) {

		EnemyInfo en = enemies.get(e.getName());

		if (en == null) {
			en = new EnemyInfo();
			enemies.put(e.getName(), en);
		}

		en.energy = e.getEnergy();
		en.live = true;
		en.bearing = pr.getHeadingRadians() + e.getBearingRadians();
		en.location = TriUtil.project(myLocation, pr.getHeadingRadians() + e.getBearingRadians(), e.getDistance());

		// normal target selection: the one closer to you is the most dangerous
		// so attack him
		if (!target.live || e.getDistance() < myLocation.distance(target.location)) {
			target = en;
		}

		// Change in one vs one mode
		if (pr.getOthers() == 1)
			;
	}

	public void onRobotDeath(RobotDeathEvent e) {
		enemies.get(e.getName()).live = false;
	}

	public void onHitByBullet(HitByBulletEvent event) {
		enemies.get(event.getName()).damage=enemies.get(event.getName()).damage+0.1;
	}

	public void onPaint(Graphics2D g) {
		g.setColor(Color.GREEN);
		g.drawRect((int)nextLocation.x, (int)nextLocation.y, 10, 10);
		
		g.setColor(Color.RED);
		g.drawRect((int)target.location.x, (int)target.location.y, 10, 10);
		
		
		ArrayList<Point2D.Double> points=new ArrayList<Point2D.Double>();
				
		Point2D.Double testPoint;
		double max=0;
		double distanceToTarget = myLocation.distance(target.location);
		int i=0;
		
		do {
			//	calculate the testPoint somewhere around the current position. 100 + 200*Math.random() proved to be good if there are
			//	around 10 bots in a 1000x1000 field. but this needs to be limited this to distanceToTarget*0.8. this way the bot wont
			//	run into the target (should mostly be the closest bot) 
			testPoint = generatePoint(distanceToTarget);
			if(battleField.contains(testPoint) ) {
				points.add(testPoint);
				if(riskEvaluation(testPoint)>max)
					max=riskEvaluation(testPoint);
			}
		} while(i++ < POINT_GEN);
		
		
		for(Point2D.Double p : points){
			int colorRisk=(int)(riskEvaluation(p)/max * 255);
			g.setColor(new Color(colorRisk,0,255-colorRisk));
			g.fillRect((int)p.x, (int)p.y, 5, 5);
		}
	}
	
	
}

class EnemyInfo {
	public Point2D.Double location;
	public double energy;
	public boolean live;
	public double bearing;
	public double damage;
}
