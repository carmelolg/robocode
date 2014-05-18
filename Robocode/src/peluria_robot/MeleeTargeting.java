package peluria_robot;

import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

import robocode.HitByBulletEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;

class Enemy{
	String name;
	double currentEnergy;
}

public class MeleeTargeting {

	PeluriaRobot pr;
	Map<String, Enemy> mapOfEnemy;
	public MeleeTargeting(PeluriaRobot pr) {
		this.pr = pr;
		mapOfEnemy = new HashMap<String, Enemy>();
	}
	
	public void onScannedRobot(ScannedRobotEvent event){
//		if(mapOfEnemy.size() < )
		
	}
	public void onPaint(Graphics2D g) {
				
	}

	public void onHitByBullet(HitByBulletEvent event) {
		
	}
	public void onRobotDeath(RobotDeathEvent e) {
		
	}
}
