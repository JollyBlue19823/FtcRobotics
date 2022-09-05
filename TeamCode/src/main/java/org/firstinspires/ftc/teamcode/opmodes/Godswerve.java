package org.firstinspires.ftc.teamcode.opmodes;

//Import EVERYTHING we need
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;import com.outoftheboxrobotics.photoncore.PhotonCore;import com.acmerobotics.dashboard.config.Config;import com.qualcomm.robotcore.hardware.AnalogInput;import com.acmerobotics.dashboard.FtcDashboard;import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;import com.qualcomm.hardware.bosch.BNO055IMU;import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;import com.qualcomm.robotcore.hardware.DcMotorEx;import com.qualcomm.robotcore.hardware.CRServo;import com.qualcomm.robotcore.util.ElapsedTime;import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;import org.firstinspires.ftc.robotcore.external.navigation.Orientation;import org.firstinspires.ftc.teamcode.maths.Controlloopmath;import org.firstinspires.ftc.teamcode.maths.mathsOperations;import org.firstinspires.ftc.robotcore.external.navigation.Position;import org.firstinspires.ftc.teamcode.maths.swerveMaths;import java.util.List;import org.firstinspires.ftc.robotcore.external.navigation.Velocity;import com.qualcomm.hardware.lynx.LynxModule;

@Config
@TeleOp(name="Godswerve", group="Linear Opmode")
public class Godswerve extends LinearOpMode {

    //Initialize all of our hardware
    private AnalogInput BLE = null, BRE = null, FLE = null, FRE = null;

    private CRServo BLT = null, BRT = null, FLT = null, FRT = null;

    private DcMotorEx BLD = null, BRD = null, FLD = null, FRD = null;

    List<LynxModule> allHubs = null;

    //Initialize FTCDashboard
    FtcDashboard dashboard;

    //Define reference points
    public static double BLTreference = 0, BRTreference=0,FLTreference=0,FRTreference=0;
    public static double BLTreference1 = 0, BRTreference1=0,FLTreference1=0,FRTreference1=0;

    //Timers for the PID loops
    ElapsedTime BLTtimer =  new ElapsedTime(); ElapsedTime FRTtimer =  new ElapsedTime(); ElapsedTime FLTtimer =  new ElapsedTime(); ElapsedTime BRTtimer =  new ElapsedTime();

    //Define values for wheel positions
    double BLP = 0, BRP = 0, FLP = 0, FRP = 0;

    //Variables for power of wheels
    double BLDpower,BRDpower,FLDpower,FRDpower;

    //Tuning values so that wheels are always facing straight (accounts for encoder drift - tuned manually)
    public static double BLPC = -100, FRPC = -5, BRPC = 87, FLPC = 190;

    //IMU
    BNO055IMU IMU;
    Orientation angles;

    public void runOpMode() {
        telemetry.addData("Status", "Initialized");

        //Calibrate the IMU
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.loggingEnabled = true;
        parameters.loggingTag = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();
        IMU = hardwareMap.get(BNO055IMU.class, "IMU");
        IMU.initialize(parameters);
        IMU.startAccelerationIntegration(new Position(), new Velocity(), 1000);

        angles   = IMU.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
        double headingfix = angles.firstAngle;

        //Initialize FTCDashboard telemetry
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        //Link all of our hardware to our hardwaremap
        BLE = hardwareMap.get(AnalogInput.class, "BLE");
        BRE = hardwareMap.get(AnalogInput.class, "BRE");
        FLE = hardwareMap.get(AnalogInput.class, "FLE");
        FRE = hardwareMap.get(AnalogInput.class, "FRE");

        BLD = hardwareMap.get(DcMotorEx.class, "BLD");
        BRD = hardwareMap.get(DcMotorEx.class, "BRD");
        FLD = hardwareMap.get(DcMotorEx.class, "FLD");
        FRD = hardwareMap.get(DcMotorEx.class, "FRD");

        BLT = hardwareMap.get(CRServo.class, "BLT");
        BRT = hardwareMap.get(CRServo.class, "BRT");
        FLT = hardwareMap.get(CRServo.class, "FLT");
        FRT = hardwareMap.get(CRServo.class, "FRT");

        //Bulk sensor reads
        allHubs = hardwareMap.getAll(LynxModule.class);

        //Initialize FTCDashboard
        dashboard = FtcDashboard.getInstance();

        //Create objects for the classes we use for swerve
        swerveMaths swavemath = new swerveMaths();

        Controlloopmath BLTPID = new Controlloopmath(0.2,0.0001,0,0,BLTtimer);
        Controlloopmath BRTPID = new Controlloopmath(0.2,0.0001,0,0,BRTtimer);
        Controlloopmath FLTPID = new Controlloopmath(0.2,0.0001,0,0,FLTtimer);
        Controlloopmath FRTPID = new Controlloopmath(0.2,0.0001,0,0,FRTtimer);

        //Bulk sensor reads
        for (LynxModule module : allHubs) {
            module.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
        }
        BLTreference = 0; BRTreference=0;FLTreference=0;FRTreference=0;

        PhotonCore.enable();

        waitForStart();
        while (opModeIsActive()) {

            //Clear the cache for better loop times (bulk sensor reads)
            for (LynxModule hub : allHubs) {
                hub.clearBulkCache();
            }

            //Turn our MA3 absolute encoder signals from volts to degrees
            BLP = BLE.getVoltage() * -74.16;
            BRP = BRE.getVoltage() * -74.16;
            FLP = FLE.getVoltage() * -74.16;
            FRP = FRE.getVoltage() * -74.16;

            //Update heading of robot
            angles   = IMU.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
            double heading = angles.firstAngle*-1;

            telemetry.addData("IMU",heading);

            //Anglewrap our positions and references for each wheel
            BLP=mathsOperations.angleWrap(BLP);
            BRP=mathsOperations.angleWrap(BRP);
            FLP=mathsOperations.angleWrap(FLP);
            FRP=mathsOperations.angleWrap(FRP);

            BLTreference=mathsOperations.angleWrap(BLTreference);
            BRTreference=mathsOperations.angleWrap(BRTreference);
            FLTreference=mathsOperations.angleWrap(FLTreference);
            FRTreference=mathsOperations.angleWrap(FRTreference);

            //put our outputs into an array

            //Retrieve the angles and powers for all of our wheels from the swerve kinematics
            double[] output = swavemath.Math(gamepad1.left_stick_y,gamepad1.left_stick_x,gamepad1.right_stick_x,heading,true);
            BRDpower=output[0];
            BLDpower=output[1];
            FRDpower=output[2];
            FLDpower=output[3];

            while (gamepad1.left_stick_y!=0||gamepad1.left_stick_x!=0||gamepad1.right_stick_x!=0&&opModeIsActive()){
                BRTreference1=output[4];
                BLTreference1=output[5];
                FRTreference1=output[6];
                FLTreference1=output[7];
                BRTreference=BRTreference1;
                BLTreference=BLTreference1;
                FRTreference=FRTreference1;
                FLTreference=FLTreference1;
                telemetry.addData("in","yes");
                break;
            }
            while(gamepad1.left_stick_y==0&gamepad1.left_stick_x==0&gamepad1.right_stick_x==0&&opModeIsActive()){

                BRTreference=BRTreference1;
                BLTreference=BLTreference1;
                FRTreference=FRTreference1;
                FLTreference=FLTreference1;

                telemetry.addData("in","na");
                break;
            }

            BLTreference=mathsOperations.angleWrap(BLTreference);
            BRTreference=mathsOperations.angleWrap(BRTreference);
            FLTreference=mathsOperations.angleWrap(FLTreference);
            FRTreference=mathsOperations.angleWrap(FRTreference);

            //Subtract our tuning values to account for any encoder drift
            FRTreference -= FRPC;
            FLTreference -= FLPC;
            BRTreference -= BRPC;
            BLTreference -= BLPC;

            BLTreference=mathsOperations.angleWrap(BLTreference);
            BRTreference=mathsOperations.angleWrap(BRTreference);
            FLTreference=mathsOperations.angleWrap(FLTreference);
            FRTreference=mathsOperations.angleWrap(FRTreference);

            //Run our powers, references, and positions through efficient turning code for each wheel and get the new values
            double[] BLTvalues= mathsOperations.efficientTurn(BLTreference,BLP,BLDpower);
            BLTreference=BLTvalues[0];
            BLDpower=BLTvalues[1];

            double[] BRTvalues = mathsOperations.efficientTurn(BRTreference,BRP,BRDpower);
            BRTreference=BRTvalues[0];
            BRDpower=BRTvalues[1];

            double[] FLTvalues = mathsOperations.efficientTurn(FLTreference,FLP,FLDpower);
            FLTreference=FLTvalues[0];
            FLDpower=FLTvalues[1];

            double[] FRTvalues = mathsOperations.efficientTurn(FRTreference,FRP,FRDpower);
            FRTreference=FRTvalues[0];
            FRDpower=FRTvalues[1];

            //Use our Controlloopmath class to find the power needed to go into our CRservo to achieve our desired target
            BLT.setPower(BLTPID.PIDout(BLTreference,BLP));
            BLD.setPower(BLDpower);

            BRT.setPower(BRTPID.PIDout(BRTreference,BRP));
            BRD.setPower(BRDpower);

            FLT.setPower(FLTPID.PIDout(FLTreference,FLP));
            FLD.setPower(FLDpower);

            FRT.setPower(FRTPID.PIDout(FRTreference,FRP));
            FRD.setPower(FRDpower);

            telemetry.addData("BLTreference",BLTreference);
            telemetry.addData("BRTreference",BRTreference);
            telemetry.addData("FLTreference",FLTreference);
            telemetry.addData("FRTreference",FRTreference);

            telemetry.addData("BLP",BLP);
            telemetry.addData("BRP",BRP);
            telemetry.addData("FLP",FLP);
            telemetry.addData("FRP",FRP);

            telemetry.addData("FRDpower",FRDpower);
            telemetry.addData("FLDpower",FLDpower);
            telemetry.addData("BRDpower",BRDpower);
            telemetry.addData("BLDpower",BLDpower);
            telemetry.update();

        }
    }
}
