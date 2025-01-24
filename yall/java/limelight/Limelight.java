package limelight;


import static limelight.structures.LimelightUtils.getLimelightURLString;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.Alert;
import edu.wpi.first.wpilibj.Alert.AlertType;
import edu.wpi.first.wpilibj.DriverStation;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import limelight.estimator.LimelightPoseEstimator;
import limelight.structures.LimelightData;
import limelight.structures.LimelightResults;
import limelight.structures.LimelightSettings;

/**
 * Limelight Camera class.
 */
public class Limelight
{

  /**
   * {@link Limelight} name.
   */
  public final String            limelightName;
  /**
   * {@link Limelight} data from NetworkTables.
   */
  private      LimelightData     limelightData;
  /**
   * {@link Limelight} settings that we apply.
   */
  private      LimelightSettings settings;


  /**
   * Constructs and configures the {@link Limelight} NT Values.
   *
   * @param name Name of the limelight
   */
  public Limelight(String name)
  {
    isAvailable(name);
    limelightName = name;
    limelightData = new LimelightData(this);
    settings = new LimelightSettings(this);
  }

  /**
   * Verify limelight name exists as a table in NT.
   * <p>
   * This check is expected to be run once during robot construction and is not intended to be checked in the iterative
   * loop.
   * <p>
   * Use check "yourLimelightObject.getData().targetData.getTargetStatus())"" for the validity of an iteration for 2d
   * targeting.
   * <p>
   * For valid 3d pose check "yourLimelightPoseEstimatorObject.getPoseEstimate().get().hasData"
   *
   * @param limelightName Limelight Name to check for table existence.
   * @return true if an NT table exists with requested LL name.
   * <p>false and issues a WPILib Error Alert if requested LL doesn't appear as an NT table.
   */
  @SuppressWarnings("resource")
  public static boolean isAvailable(String limelightName)
  {
    // LL sends key "getpipe" if it's on so check that
    // put in a delay if needed to help assure NT has latched onto the LL if it is transmitting
    for (int i = 1; i <= 15; i++)
    {
      if (NetworkTableInstance.getDefault().getTable(limelightName).containsKey("getpipe"))
      {
        return true;
      }
      System.out.println("waiting " + i + " of 15 seconds for limelight to attach");
      try
      {
        Thread.sleep(1000);
      } catch (InterruptedException e)
      {
        e.printStackTrace();
      }
    }

    StringBuilder message = new StringBuilder();
    message.append("Your limelight name \"");
    message.append(limelightName);
    message.append("\" doesn't exist on the network (no getpipe key).\nThese may be available <");
    var foundNoLL = true;
    var NTtables  = NetworkTableInstance.getDefault().getTable("/").getSubTables().toArray();
    for (Object element : NTtables)
    {
      var tableName = (String) element;
      if (tableName.startsWith("limelight"))
      {
        foundNoLL = false;
        message.append(tableName);
        message.append(" ");
      }
    }

    if (foundNoLL)
    {
      message.append("--none--");
    }

    message.append(">");

    DriverStation.reportError(message.toString(), false);
    new Alert(message.toString(), AlertType.kError).set(true);

    return false;
  }

  /**
   * Create a {@link LimelightPoseEstimator} for the {@link Limelight}.
   *
   * @param megatag2 Use MegaTag2.
   * @return {@link LimelightPoseEstimator}
   */
  public LimelightPoseEstimator getPoseEstimator(boolean megatag2)
  {
    return new LimelightPoseEstimator(this, megatag2);
  }

  /**
   * Get the {@link LimelightSettings} preparatory to changing settings.
   * <p>While this method does get current settings from the LL there are
   * no getters provided for the settings so they are useless, dead data. This merely provides a stub for the various
   * ".withXXXX" settings to attach to.
   * <p>This method may be used as often as needed and may contain none or more
   * chained ".withXXXX" settings.
   *
   * @return object used as the target of the various ".withXXXX" settings methods.
   */
  public LimelightSettings settingsBuilder()
  {
    return settings;
  }

  /**
   * Get the {@link LimelightData} object for the {@link Limelight}
   *
   * @return {@link LimelightData} object.
   */
  public LimelightData getData()
  {
    return limelightData;
  }

  /**
   * Asynchronously take a snapshot in limelight.
   *
   * @param snapshotname Snapshot name to save.
   */
  public void snapshot(String snapshotname)
  {
    CompletableFuture.supplyAsync(() -> {
      URL url = getLimelightURLString(limelightName, "capturesnapshot");
      try
      {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        if (snapshotname != null && snapshotname != "")
        {
          connection.setRequestProperty("snapname", snapshotname);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == 200)
        {
          return true;
        } else
        {
          System.err.println("Bad LL Request");
        }
      } catch (IOException e)
      {
        System.err.println(e.getMessage());
      }
      return false;
    });
  }

  /**
   * Gets the latest JSON {@link LimelightResults} output and returns a LimelightResults object.
   *
   * @return LimelightResults object containing all current target data
   */
  public Optional<LimelightResults> getLatestResults()
  {
    return limelightData.getResults();
  }

  /**
   * Flush the NetworkTable data to server.
   */
  public void flush()
  {
    NetworkTableInstance.getDefault().flush();
  }

  /**
   * Get the {@link NetworkTable} for this limelight.
   *
   * @return {@link NetworkTable} for this limelight.
   */
  public NetworkTable getNTTable()
  {
    return NetworkTableInstance.getDefault().getTable(limelightName);
  }
}
