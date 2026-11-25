package yall;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import yall.sim.CameraModel;

class CameraModelTest {

  @ParameterizedTest
  @EnumSource(CameraModel.class)
  void simulatesThePublishedSpecs(CameraModel model) {
    var properties = model.getSimCameraProperties();
    assertEquals(model.getWidthPixels(), properties.getResWidth());
    assertEquals(model.getHeightPixels(), properties.getResHeight());
    assertEquals(model.getHorizontalFovDegrees(), properties.getHorizFOV().getDegrees(), 0.01);
    assertEquals(model.getVerticalFovDegrees(), properties.getVertFOV().getDegrees(), 0.01);
    assertEquals(model.getFps(), properties.getFPS(), 1e-9);
  }
}
