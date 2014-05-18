package peluria_robot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;

import robocode.HitByBulletEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.TurnCompleteCondition;
import robocode.util.Utils;

public class MinimumRiskMovement {
	
	static HashMap<String, EnemyInfo> enemies=new HashMap<String, EnemyInfo>();
	static EnemyInfo target;
	static Point2D.Double nextLocation;
	static Point2D.Double lastLocation;
	static Point2D.Double myLocation;
	static double myEnergy;
	
	static Rectangle2D.Double battleField;
	
	PeluriaRobot pr;
	
	public MinimumRiskMovement(PeluriaRobot pr) {
		this.pr=pr;

	}
	
	public void init(){
		myLocation = new Point2D.Double(pr.getX(), pr.getY());
		target = new EnemyInfo();
		nextLocation = myLocation;
		lastLocation = myLocation;
		battleField = new Rectangle2D.Double(18, 18, pr.getBattleFieldWidth() - 36, pr.getBattleFieldHeight() - 36);

	}
	
	public void run(){

		myLocation = new Point2D.Double(pr.getX(), pr.getY());
		myEnergy = pr.getEnergy();
		pr.setTurnRadarRightRadians(2*Math.PI);

		
		if(target.live && pr.getTime()>9) {
			doMovementAndGun();
		}
	}

	private void doMovementAndGun() {

		double distanceToTarget = myLocation.distance(target.location);
		double distanceToNextDestination = myLocation.distance(nextLocation);

		//search a new destination if I reached this one
		if(distanceToNextDestination < 15) {
			
			// there should be better formulas then this one but it is basically here to increase OneOnOne performance. with more bots
			// addLast will mostly be 1
			double addLast = 1 - Math.rint(Math.pow(Math.random(), pr.getOthers()));
			
			Point2D.Double testPoint;
			int i=0;
			
			do {
				//	calculate the testPoint somewhere around the current position. 100 + 200*Math.random() proved to be good if there are
				//	around 10 bots in a 1000x1000 field. but this needs to be limited this to distanceToTarget*0.8. this way the bot wont
				//	run into the target (should mostly be the closest bot) 
				testPoint = TriUtil.project(myLocation, 2*Math.PI*Math.random(), Math.min(distanceToTarget*0.8, 100 + 200*Math.random()));
				if(battleField.contains(testPoint) && riskEvaluation (testPoint, addLast) < riskEvaluation (nextLocation, addLast)) {
					nextLocation = testPoint;
				}
			} while(i++ < 200);
			
			lastLocation = myLocation;
			
		} else {
			
		// just the normal goTo stuff
			double angle = TriUtil.absoluteBearing(myLocation, nextLocation) - pr.getHeadingRadians();
			double direction = 1;
			
			if(Math.cos(angle) < 0) {
				angle += Math.PI;
				direction = -1;
			}
			
			pr.setAhead(distanceToNextDestination * direction);
			pr.setTurnRightRadians(angle = Utils.normalRelativeAngle(angle));
			// hitting walls isn't a good idea, but HawkOnFire still does it pretty often
			pr.setMaxVelocity(Math.abs(angle) > 1 ? 0 : 8d);
			
		}
		
	}
	
	
	
	public static double riskEvaluation (Point2D.Double point, double addLast) {
		// this is basically here that the bot uses more space on the battlefield. In melee it is dangerous to stay somewhere too long.
		double eval = addLast*0.08/point.distanceSq(lastLocation);
		
		for (String key:enemies.keySet()) {
			EnemyInfo en = enemies.get(key);
			// this is the heart of HawkOnFire. So I try to explain what I wanted to do:
			// -	Math.min(en.energy/myEnergy,2) is multiplied because en.energy/myEnergy is an indicator how dangerous an enemy is
			// -	Math.abs(Math.cos(calcAngle(myPos, p) - calcAngle(en.pos, p))) is bigger if the moving direction isn't good in relation
			//		to a certain bot. it would be more natural to use Math.abs(Math.cos(calcAngle(p, myPos) - calcAngle(en.pos, myPos)))
			//		but this wasn't going to give me good results
			// -	1 / p.distanceSq(en.pos) is just the normal anti gravity thing
			if(en.live) {
				eval += Math.min(en.energy/myEnergy,2) * 
						(1 + Math.abs(Math.cos(TriUtil.absoluteBearing(point, myLocation) - TriUtil.absoluteBearing(point, en.location)))) / point.distanceSq(en.location);
			}
		}
		return eval;
	}
	
	public void onScannedRobot(ScannedRobotEvent e)
	{
		EnemyInfo en = enemies.get(e.getName());
		
		if(en == null){
			en = new EnemyInfo();
			enemies.put(e.getName(), en);
		}
		
		en.energy = e.getEnergy();
		en.live = true;
		en.location = TriUtil.project(myLocation, pr.getHeadingRadians() + e.getBearingRadians(), e.getDistance());
		
		// normal target selection: the one closer to you is the most dangerous so attack him
		if(!target.live || e.getDistance() < myLocation.distance(target.location)) {
			target = en;
		}
		
		// locks the radar if there is only one opponent left
		if(pr.getOthers()==1)	pr.setTurnRadarLeftRadians(pr.getRadarTurnRemainingRadians());
	}
	
//- minor events ----------------------------------------------------------------------------------------------------------------------------
	public void onRobotDeath(RobotDeathEvent e) {
		enemies.get(e.getName()).live=false;
	}
	
	public void onHitByBullet(HitByBulletEvent event) {
	}
	
	public void onPaint(Graphics2D g) {
		g.setColor(Color.GREEN);
		g.drawRect((int)nextLocation.x, (int)nextLocation.y, 10, 10);
		
		
		double addLast = 1 - Math.rint(Math.pow(Math.random(), pr.getOthers()));
		
		Point2D.Double testPoint;
		int i=0;
		double distanceToTarget = myLocation.distance(target.location);
		double max=0;

		ArrayList<Point2D.Double> points=new ArrayList<Point2D.Double>();
		do {
			//	calculate the testPoint somewhere around the current position. 100 + 200*Math.random() proved to be good if there are
			//	around 10 bots in a 1000x1000 field. but this needs to be limited this to distanceToTarget*0.8. this way the bot wont
			//	run into the target (should mostly be the closest bot) 
			testPoint = TriUtil.project(myLocation, 2*Math.PI*Math.random(), Math.min(distanceToTarget*0.8, 100 + 200*Math.random()));
			if(battleField.contains(testPoint))
				points.add(testPoint);
			if(riskEvaluation(testPoint, addLast)>max)
				max=riskEvaluation(testPoint, addLast);
		} while(i++ < 200);
		
		for(Point2D.Double p : points){
			g.setColor(new Color((int)(riskEvaluation(p, addLast)/max * 255),100,255));
			g.fillRect((int)p.x, (int)p.y, 5, 5);
		}
	}

}

class EnemyInfo {
	public Point2D.Double location;
	public double energy;
	public boolean live;
}
