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

	GuessFactorTargeting gft = new GuessFactorTargeting(this);
	MinimumRiskMovement wsm = new MinimumRiskMovement(this);
//	WaveSurfingMovement wsm=new WaveSurfingMovement(this);
	MeleeTargeting mt = new MeleeTargeting(this, gft);

	public void run() {
		wsm.init();

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);

		turnRadarRightRadians(2*Math.PI);
		
		for(;;){
			wsm.run();
			execute();
		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent event) {
		wsm.onScannedRobot(event);
		mt.onScannedRobot(event);
	}

	@Override
	public void onHitByBullet(HitByBulletEvent event) {
		wsm.onHitByBullet(event);
		mt.onHitByBullet(event);
	}
	
	@Override
	public void onRobotDeath(RobotDeathEvent e) {
		wsm.onRobotDeath(e);
		mt.onRobotDeath(e);
	}

	
	@Override
	public void onPaint(Graphics2D g) {
		wsm.onPaint(g);
		mt.onPaint(g);
	}
}
