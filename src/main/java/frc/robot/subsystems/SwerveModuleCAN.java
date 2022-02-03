// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.can.TalonFX;
import com.ctre.phoenix.sensors.CANCoder;
import com.ctre.phoenix.sensors.SensorTimeBase;
import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.revrobotics.SparkMaxRelativeEncoder.Type;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj.AnalogEncoder;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.RobotController;
import frc.robot.Constants;
import frc.robot.util;
import frc.robot.Constants.MK4IModuleConstants;

public class SwerveModuleCAN {
  private final TalonFX m_driveMotor;
  private final TalonFX m_turningMotor;

  //private final RelativeEncoder m_driveEncoder;
  private final CANCoder m_turningEncoder;
  //private final AnalogInput m_offsetEncoder;

  private double turningMotorOffset;

  private final PIDController m_drivePIDController =
      new PIDController(0.0, 0, 0);

  // Using a TrapezoidProfile PIDController to allow for smooth turning
  // private final ProfiledPIDController m_turningPIDController =
  //     new ProfiledPIDController(
  //         MK4IModuleConstants.i_kPModuleTurningController,
  //         0,
  //         0.0001,
  //         new TrapezoidProfile.Constraints(
  //             MK4IModuleConstants.i_kMaxModuleAngularSpeedRadiansPerSecond,
  //             MK4IModuleConstants.i_kMaxModuleAngularAccelerationRadiansPerSecondSquared));
  private final PIDController m_turningPIDController = new PIDController(MK4IModuleConstants.i_kPModuleTurningController, 0, 0.0);

  /**
   * Constructs a SwerveModule.
   *
   * @param driveMotorChannel ID for the drive motor.
   * @param turningMotorChannel ID for the turning motor.
   */
  public SwerveModuleCAN(
      int driveMotorChannel,
      int turningMotorChannel,
      int CANEncoderPort,
      double turningMotorOffset) {
    m_driveMotor = new TalonFX(driveMotorChannel);
    m_turningMotor = new TalonFX(turningMotorChannel);
    this.turningMotorOffset = turningMotorOffset;

    //this.m_driveEncoder = new Encoder(driveEncoderPorts[0], driveEncoderPorts[1]);
    m_driveMotor.configFactoryDefault();
    m_turningMotor.configFactoryDefault();
    m_driveMotor.setNeutralMode(NeutralMode.Brake);
    m_turningMotor.setNeutralMode(NeutralMode.Brake);
  
    m_turningEncoder = new CANCoder(CANEncoderPort);
    m_turningEncoder.configFeedbackCoefficient(2 * Math.PI / MK4IModuleConstants.i_kEncoderCPR, "rad", SensorTimeBase.PerSecond);
    //m_turningEncoder = m_driveMotor.getEncoder();

    // Set the distance per pulse for the drive encoder. We can simply use the
    // distance traveled for one rotation of the wheel divided by the encoder
    // resolution.
    //m_driveMotor.setPositionConversionFactor(Constants.MK4IModuleConstants.i_kDriveEncoderDistancePerPulse);

    //m_turningEncoder.setPosition(turningMotorOffset);


    // Set whether drive encoder should be reversed or not
    // m_driveEncoder.setReverseDirection(driveEncoderReversed);

    // Set the distance (in this case, angle) per pulse for the turning encoder.
    // This is the the angle through an entire rotation (2 * pi) divided by the
    // encoder resolution.

    // Set whether turning encoder should be reversed or not
    // m_turningEncoder.setReverseDirection(turningEncoderReversed);

    // Limit the PID Controller's input range between -pi and pi and set the input
    // to be continuous.
    m_turningPIDController.enableContinuousInput(-Math.PI, Math.PI);
  }

  private double getTurningEncoderRadians(){
    double angle = m_turningEncoder.getAbsolutePosition() + turningMotorOffset;
    angle %= 2.0 * Math.PI;
    if (angle < 0.0) {
        angle += 2.0 * Math.PI;
    }
    return angle;
    }

  /**
   * Returns the current state of the module.
   *
   * @return The current state of the module.
   */
  public SwerveModuleState getState() {
    return new SwerveModuleState(m_driveMotor.getSelectedSensorVelocity(), new Rotation2d(getTurningEncoderRadians()));
  }

  /**
   * Sets the desired state for the module.
   *
   * @param desiredState Desired state with speed and angle.
   */
  public void setDesiredState(SwerveModuleState desiredState) {
    // Optimize the reference state to avoid spinning further than 90 degrees
    SwerveModuleState state =
        SwerveModuleState.optimize(desiredState, new Rotation2d(getTurningEncoderRadians()));

    // Calculate the drive output from the drive PID controller.
    final double driveOutput =
        state.speedMetersPerSecond/util.feetToMeters(12);//ontroller.calculate(m_driveEncoder.getVelocity(), state.speedMetersPerSecond);

    // Calculate the turning motor output from the turning PID controller.
    final var turnOutput =
        m_turningPIDController.calculate(getTurningEncoderRadians(), state.angle.getRadians());

    // Calculate the turning motor output from the turning PID controller.
    m_driveMotor.set(ControlMode.PercentOutput, driveOutput);
    m_turningMotor.set(ControlMode.PercentOutput, turnOutput);
  }

  /** Zeros all the SwerveModule encoders. */
  /*public void resetEncoders() {
    m_driveEncoder.reset();
    m_turningEncoder.reset();
  }*/
}
