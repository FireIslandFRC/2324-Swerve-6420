package frc.robot.subsystem.drivetrain;

import com.revrobotics.CANSparkMax;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.RobotState;
import frc.lib.GlobalConstants;
import frc.robot.Robot;
import org.littletonrobotics.junction.Logger;

class SwerveModule {
    private final SwerveModuleIO module;

    private final PIDController driveController;
    private final PIDController steerController;

    private SwerveModuleState state;
    private SwerveModulePosition position;

    private SpeedMode mode;
    private final SwerveModuleDetails details;

    public SwerveModule(SwerveModuleDetails details)
    {
        driveController = new PIDController(0,0,0);
        driveController.setTolerance(10);

        steerController = new PIDController(0,0,0);
        steerController.enableContinuousInput(-Math.PI, Math.PI);

        if (Robot.isReal())
        {
            driveController.setP(DrivetrainConstants.DRIVE_CONTROLLER_REAL.kP);
            driveController.setI(DrivetrainConstants.DRIVE_CONTROLLER_REAL.kI);
            driveController.setD(DrivetrainConstants.DRIVE_CONTROLLER_REAL.kD);

            steerController.setP(DrivetrainConstants.STEER_CONTROLLER_REAL.kP);
            steerController.setI(DrivetrainConstants.STEER_CONTROLLER_REAL.kI);
            steerController.setD(DrivetrainConstants.STEER_CONTROLLER_REAL.kD);
        } else if (Robot.isSimulation()) {

            driveController.setP(DrivetrainConstants.DRIVE_CONTROLLER_SIM.kP);
            driveController.setI(DrivetrainConstants.DRIVE_CONTROLLER_SIM.kI);
            driveController.setD(DrivetrainConstants.DRIVE_CONTROLLER_SIM.kD);

            steerController.setP(DrivetrainConstants.STEER_CONTROLLER_SIM.kP);
            steerController.setI(DrivetrainConstants.STEER_CONTROLLER_SIM.kI);
            steerController.setD(DrivetrainConstants.DRIVE_CONTROLLER_SIM.kD);
        }else{
            throw new RuntimeException(
                    "The PID controls for both the drive controller " +
                    "and the steer controllers are not getting configured, " +
                    "how is the robot not real or simulated"
            );
        }

        steerController.setTolerance(.01);

        position = new SwerveModulePosition();

        state = new SwerveModuleState();

        if (Robot.isSimulation())
        {
            module = new SwerveModuleSim(details);
        }else {
            module = new SwerveModuleRev(details);
        }

        this.details = details;

        module.setState(new SwerveModuleState(0,new Rotation2d()));
        setIdleMode(CANSparkMax.IdleMode.kBrake);
    }

    public void setIdleMode(CANSparkMax.IdleMode mode)
    {
        module.setIdleMode(mode);
    }

    void periodic(){
        module.setState(state);
        module.update();

        double speedOfMotorRPM = state.speedMetersPerSecond;

        if (mode == SpeedMode.slow)
        {
            speedOfMotorRPM = speedOfMotorRPM / DrivetrainConstants.SLOW_SPEED;
        } else if (mode == SpeedMode.normal) {
            speedOfMotorRPM = speedOfMotorRPM / DrivetrainConstants.NORMAL_SPEED;
        }
        speedOfMotorRPM = speedOfMotorRPM * GlobalConstants.NEO_MAX_RPM;

        module.setDriveVoltage(
                driveController.calculate(module.getWheelVelocity(), speedOfMotorRPM)
        );
        module.setSteerVoltage(
                steerController.calculate(MathUtil.angleModulus(module.getWheelAngle().getRadians()), MathUtil.angleModulus(state.angle.getRadians()))
        );
        Logger.getInstance().recordOutput("Drivetrain/" + details.module.name() + "/TargetVelocity", speedOfMotorRPM);
        Logger.getInstance().recordOutput("Drivetrain/" + details.module.name() + "/TargetAngle", state.angle.getRadians());
        Logger.getInstance().recordOutput("Drivetrain/" + details.module.name() + "/AtAngleSetpoint", steerController.atSetpoint());

        if (RobotState.isDisabled())
        {
            steerController.reset();

            driveController.reset();

        }

        position = new SwerveModulePosition(module.getWheelPosition(),module.getWheelAngle());

    }

    void setState(SwerveModuleState state)
    {
        this.state = state;
    }

    void resetModulePositions()
    {
        module.resetPosition();
        position = new SwerveModulePosition();
    }

    void setSpeedMode(SpeedMode mode)
    {
        this.mode = mode;
    }
    public SwerveModulePosition getModulePosition()
    {
        return position;
    }

    SwerveModuleState getState()
    {
        double speedOfWheel = module.getWheelVelocity();
        speedOfWheel = speedOfWheel / GlobalConstants.NEO_MAX_RPM;

        if (mode == SpeedMode.normal)
        {
            speedOfWheel = speedOfWheel * DrivetrainConstants.NORMAL_SPEED;
        } else if (mode == SpeedMode.slow) {
            speedOfWheel = speedOfWheel * DrivetrainConstants.SLOW_SPEED;
        }

        return new SwerveModuleState(speedOfWheel, module.getWheelAngle());
    }
}
