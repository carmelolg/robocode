package peluria_robot;

import java.awt.Color;
import java.awt.Graphics2D;

import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;

/**
 * @author Peluria Brothers
 */
public class PeluriaRobot extends AdvancedRobot {

	BotMovement movement = new MinimumRiskMovement(this);
	BotTargeting targeting = new BotTargeting(this, movement);

	public void run() {
		movement.init();

		// Sets the radar and gun to turn independent from the robot's turn.
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForRobotTurn(true);

		// Sets the radar to turn independent from the gun robot.
		setAdjustRadarForGunTurn(true);

		// Set Peluria-Bot battle skin
		setColors(Color.WHITE, Color.BLACK, Color.BLACK);
		setBulletColor(Color.PINK);
		setScanColor(Color.PINK);

		for (;;) {
			movement.run();
			execute();
		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent event) {
		movement.onScannedRobot(event);
		targeting.onScannedRobot(event);
	}

	@Override
	public void onHitByBullet(HitByBulletEvent event) {
		movement.onHitByBullet(event);
		targeting.onHitByBullet(event);
	}

	@Override
	public void onRobotDeath(RobotDeathEvent e) {
		movement.onRobotDeath(e);
		targeting.onRobotDeath(e);
	}

	@Override
	public void onPaint(Graphics2D g) {
		movement.onPaint(g);
		targeting.onPaint(g);
	}

	// Change to 1 vs 1 movement
	public void changeWaveMovement() {
		if (movement instanceof WaveSurfingMovement)
			return;
		movement = new WaveSurfingMovement(this);
		movement.init();
	}
}
