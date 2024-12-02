package limelight;


import static limelight.structures.LimelightUtils.getLimelightURLString;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import limelight.estimator.LimelightPoseEstimator;
import limelight.structures.LimelightData;
import limelight.structures.LimelightResults;
import limelight.structures.LimelightSettings;

public class Limelight
{

  /**
   * {@link Limelight} name.
   */
  public final String            limelightName;
  /**
   * Object mapper for the {@link LimelightResults} class.
   */
  private      ObjectMapper      resultsObjectMapper;
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
   * @param name Name of the limelight.
   */
  public Limelight(String name)
  {
    LimelightResults results = new LimelightResults();
    limelightName = name;
    limelightData = new LimelightData(this);
    settings = new LimelightSettings(this);
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
   * Get the {@link LimelightSettings} with current selections.
   */
  public LimelightSettings getSettings()
  {
    return settings;
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
   * @param limelightName Name of the {@link Limelight} camera
   * @return LimelightResults object containing all current target data
   */
  public Optional<LimelightResults> getLatestResults(String limelightName)
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
