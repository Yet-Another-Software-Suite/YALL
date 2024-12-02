package frc.robot.limelight;

import static frc.robot.limelight.LimelightHelpers.getLimelightURLString;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import frc.robot.limelight.structures.LimelightData;
import frc.robot.limelight.structures.LimelightResults;
import frc.robot.limelight.structures.LimelightSettings;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class Limelight
{

  /**
   * Limelight name.
   */
  public final String       limelightName;
  /**
   * Object mapper for the {@link LimelightResults} class.
   */
  private      ObjectMapper resultsObjectMapper;
  /**
   * Limelight data from NetworkTables.
   */
  private LimelightData limelightData;
  /**
   * Limelight settings that we apply.
   */
  private LimelightSettings settings;


  /**
   * Constructs and configures the {@link Limelight} NT Values.
   *
   * @param name Name of the limelight.
   */
  public Limelight(String name)
  {
    limelightName = name;
    limelightData = new LimelightData(this);
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
   * Gets the latest JSON results output and returns a LimelightResults object.
   *
   * @param limelightName Name of the Limelight camera
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
   * @return {@link NetworkTable} for this limelight.
   */
  public NetworkTable getNTTable()
  {
    return NetworkTableInstance.getDefault().getTable(limelightName);
  }
}
