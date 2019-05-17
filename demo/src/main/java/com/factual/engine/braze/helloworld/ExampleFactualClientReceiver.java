package com.factual.engine.braze.helloworld;

import android.util.Log;
import com.factual.engine.FactualClientReceiver;
import com.factual.engine.api.FactualCircumstance;
import com.factual.engine.api.FactualConfigMetadata;
import com.factual.engine.api.FactualError;
import com.factual.engine.api.FactualInfo;
import com.factual.engine.braze.BrazeEngineIntegration;

public class ExampleFactualClientReceiver extends FactualClientReceiver {
  @Override
  public void onStarted() {
    Log.i("engine", "Engine has started.");
    BrazeEngineIntegration.initializeBrazeEngineIntegration(getContext().getApplicationContext());
  }

  @Override
  public void onStopped() {
    Log.i("engine", "Engine has stopped.");
  }

  @Override
  public void onError(FactualError e) {
    Log.i("engine", e.getMessage());
  }

  @Override
  public void onInfo(FactualInfo i) {
    Log.i("engine", i.getInfo());
  }

  @Override
  public void onSyncWithGarageComplete() {
    Log.i("engine", "Garage synced.");
  }

  @Override
  public void onConfigLoad(FactualConfigMetadata data) {
    Log.i("engine", "Garage config loaded at: " + data.getVersion());
    if (data.getGarageRelease() != null) {
      for (FactualCircumstance circumstance : data.getGarageRelease().getCircumstances()){
        Log.i("engine", "loaded circumstance: " + circumstance.getCircumstanceId());
      }
    } else {
      Log.i("engine", "Garage release is empty");
    }
  }

  @Override
  public void onDiagnosticMessage(String diagnosticMessage) {
    Log.i("engine", diagnosticMessage);
  }
}
