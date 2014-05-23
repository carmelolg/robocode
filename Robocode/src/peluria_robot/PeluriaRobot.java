package peluria_robot;

import java.awt.Graphics2D;

import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;

/**
 * @author Peluria Brothers
 */
public class PeluriaRobot extends AdvancedRobot {

	GuessFactorTargeting mt = new GuessFactorTargeting(this);
	BotMovement movement = new MinimumRiskMovement(this);
//	WaveSurfingMovement wsm=new WaveSurfingMovement(this);
//	MeleeTargeting mt = new MeleeTargeting(this, gft);

	public void run() {
		movement.init();

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);

		setTurnRadarRightRadians(2*Math.PI);
		
		for(;;){
			movement.run();
			execute();
		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent event) {
		movement.onScannedRobot(event);
		mt.onScannedRobot(event);
	}

	@Override
	public void onHitByBullet(HitByBulletEvent event) {
		movement.onHitByBullet(event);
//		mt.onHitByBullet(event);
	}
	
	@Override
	public void onRobotDeath(RobotDeathEvent e) {
		movement.onRobotDeath(e);
//		mt.onRobotDeath(e);
	}

	
	@Override
	public void onPaint(Graphics2D g) {
		movement.onPaint(g);
		mt.onPaint(g);
	}
	
	public void changeWaveMovement(){
		if(movement instanceof WaveSurfingMovement)return;
		movement = new WaveSurfingMovement(this);
		movement.init();
	}
}
