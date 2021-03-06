package com.factual.engine.braze;

import android.content.Context;

import android.text.TextUtils;
import android.util.Log;
import com.appboy.Appboy;
import com.appboy.models.outgoing.AppboyProperties;
import com.factual.engine.integrationutils.PlaceCategoryMap;
import com.factual.engine.integrationutils.PlaceChainMap;
import com.factual.engine.api.CircumstanceResponse;
import com.factual.engine.api.FactualPlace;
import java.util.List;
import java.util.UUID;

public class BrazeEngineIntegration {

  private static Boolean trackingSpans = false;

  // Constants & keys
  static final String sourceName = "factual";

  static final String TAG = "BrazeEngine";

  static final int NUM_MAX_AT_PLACE_EVENTS_PER_CIRCUMSTANCE_DEFAULT = 10;
  static final int NUM_MAX_NEAR_PLACE_EVENTS_PER_CIRCUMSTANCE_DEFAULT = 20;
  static final int NUM_MAX_ATTACHED_PLACE_EVENTS_PER_SPAN_DEFAULT = 20;
  static final String NUM_MAX_ATTACHED_PLACE_EVENTS_PER_SPAN_KEY = "max_attached_place_events_per_span";

  static final String ENGINE_SPAN_EVENT_KEY = "engine_span_occurred";
  static final String ENGINE_SPAN_ATTACHED_PLACE_EVENT_KEY = "engine_span_attached_place";

  static final String CIRCUMSTANCE_MET_EVENT_KEY = "engine_";
  static final String INCIDENT_ID_KEY = "incidence_id";
  static final String TAGS_KEY = "tags";
  static final String USER_LATITUDE_KEY = "user_latitude";
  static final String USER_LONGITUDE_KEY = "user_longitude";
  static final String EVENT_SOURCE_KEY = "event_source";

  static final String AT_PLACE_EVENT_KEY = "engine_at_";
  static final String NEAR_PLACE_EVENT_KEY = "engine_near_";
  static final String PLACE_NAME_KEY = "name";
  static final String PLACE_ID_KEY = "factual_id";
  static final String PLACE_LATITUDE_KEY = "latitude";
  static final String PLACE_LONGITUDE_KEY = "longitude";
  static final String PLACE_CATEGORIES_KEY = "category_labels";
  static final String PLACE_CHAIN_KEY = "chain_name";

  private BrazeEngineIntegration() {}

  /**
   * <b>WARNING: Braze must be initialized prior to executing this method. </b>
   * <p>
   * Sends a Circumstance Response to Braze as a custom event.
   *
   * <p>
   * The following custom events may be sent:
   * <br>
   * a) Event named {@value #CIRCUMSTANCE_MET_EVENT_KEY} <i>+ CIRCUMSTANCE NAME</i> to represent the
   * triggered circumstance
   * <br>
   * b) At most {@value #NUM_MAX_AT_PLACE_EVENTS_PER_CIRCUMSTANCE_DEFAULT} events named {@value
   * #AT_PLACE_EVENT_KEY} <i>+ CIRCUMSTANCE NAME</i> for each place the user is at which
   * triggered the event.
   * <br>
   * c) At most {@value #NUM_MAX_NEAR_PLACE_EVENTS_PER_CIRCUMSTANCE_DEFAULT} events named {@value
   * #NEAR_PLACE_EVENT_KEY} <i>+ CIRCUMSTANCE NAME</i> for each place the user is near which
   * triggered the event.
   *
   * <p>
   * Based on the generality of the circumstance definition, Factual's Engine SDK may match multiple
   * potential candidate places the user may have been at or near when the circumstance was
   * triggered.
   *
   * @param response A single circumstance response from onCircumstancesMet
   */
  public static void pushToBraze(Context context, CircumstanceResponse response) {
    // push to braze with a default number of place events
    pushToBraze(context,
        response,
        NUM_MAX_AT_PLACE_EVENTS_PER_CIRCUMSTANCE_DEFAULT,
        NUM_MAX_NEAR_PLACE_EVENTS_PER_CIRCUMSTANCE_DEFAULT);
  }

  /**
   * @param maxAtPlaceEvents max number of at place events to send
   * @param maxNearPlaceEvents max number of near place events to send
   * @see BrazeEngineIntegration#pushToBraze(Context, CircumstanceResponse)
   */
  public static void pushToBraze(Context context,
      CircumstanceResponse response,
      int maxAtPlaceEvents,
      int maxNearPlaceEvents) {
    Appboy appboy = Appboy.getInstance(context);
    AppboyProperties properties = new AppboyProperties();

    // Get circumstance data
    String circumstanceName = response.getCircumstance().getName();
    String tags = TextUtils.join(", ", response.getCircumstance().getTags());
    double userLatitude = response.getLocation().getLatitude();
    double userLongitude = response.getLocation().getLongitude();
    String incidentId = UUID.randomUUID().toString();

    // Add data properties
    properties.addProperty(INCIDENT_ID_KEY, incidentId)
        .addProperty(TAGS_KEY, tags)
        .addProperty(USER_LATITUDE_KEY, userLatitude)
        .addProperty(USER_LONGITUDE_KEY, userLongitude)
        .addProperty(EVENT_SOURCE_KEY, sourceName);

    // Push custom event to braze
    Log.i(TAG, String.format("Sending circumstance %s event to Braze", circumstanceName));
    String circumstanceEventName = CIRCUMSTANCE_MET_EVENT_KEY + circumstanceName;
    appboy.logCustomEvent(circumstanceEventName, properties);

    // Send any atPlaces data
    List<FactualPlace> atPlaces = response.getAtPlaces();
    int numAtPlaceEvents = Math.min(maxAtPlaceEvents, atPlaces.size());
    if (numAtPlaceEvents > 0) {
      Log.i(TAG, String.format("Sending %d at place event(s) to Braze", numAtPlaceEvents));
      String atPlaceEventName = AT_PLACE_EVENT_KEY + circumstanceName;
      sendPlacesData(atPlaces, atPlaceEventName, numAtPlaceEvents, appboy, properties);
    }
    // Send any nearPlaces data
    List<FactualPlace> nearPlaces = response.getNearPlaces();
    int numNearPlaceEvents = Math.min(maxNearPlaceEvents, nearPlaces.size());
    if (numNearPlaceEvents > 0) {
      Log.i(TAG, String.format("Sending %d near place event(s) to Braze", numNearPlaceEvents));
      String nearPlaceEventName = NEAR_PLACE_EVENT_KEY + circumstanceName;
      sendPlacesData(nearPlaces, nearPlaceEventName, numNearPlaceEvents, appboy, properties);
    }
  }

  // Send places data to braze as custom events
  private static void sendPlacesData(List<FactualPlace> places,
      String eventName,
      int numPlaceEvents,
      Appboy appboy,
      AppboyProperties properties) {

    // Loop through each place
    for (int index = 0; index < numPlaceEvents; index++) {
      FactualPlace place = places.get(index);

      // Get place data
      String categories = TextUtils.join(", ", PlaceCategoryMap.getPlaceCategories(place));
      String chain = PlaceChainMap.getChain(place);

      // Add properties
      properties.addProperty(PLACE_NAME_KEY, place.getName())
          .addProperty(PLACE_ID_KEY, place.getFactualId())
          .addProperty(PLACE_LATITUDE_KEY, place.getLatitude())
          .addProperty(PLACE_LONGITUDE_KEY, place.getLongitude())
          .addProperty(PLACE_CATEGORIES_KEY, categories)
          .addProperty(PLACE_CHAIN_KEY, chain);

      // Push custom event to braze
      appboy.logCustomEvent(eventName, properties);
    }
  }

  /**
   * <b>WARNING: Both Braze and Engine must be individually initialized and you must set
   * Engine's User Journey Receiver to BrazeEngineUserJourneyReceiver prior to executing this
   * method.  </b>
   * <p>
   * Configures Engine to push User Journey Spans to Braze as custom events.
   *
   * <p>
   * When a span occurs the following custom events will be pushed:
   * <br>
   * a) Event named {@value #ENGINE_SPAN_EVENT_KEY} to represent the span
   * <br>
   * b) At most {@value #NUM_MAX_ATTACHED_PLACE_EVENTS_PER_SPAN_DEFAULT} events named {@value
   * #ENGINE_SPAN_ATTACHED_PLACE_EVENT_KEY} for each attached place the user was at or near during
   * the span.
   *
   * <p>
   * Based on the specificity of the attached places expression there may be multiple
   * places that are listed in the span.
   */
  public static void trackUserJourneySpans(Context context) {
    // Track user journey spans with default number of attached places
    trackUserJourneySpans(context, NUM_MAX_ATTACHED_PLACE_EVENTS_PER_SPAN_DEFAULT);
  }

  /**
   * @param numMaxAttachedPlaceEventsPerSpan max number of custom events to push for each place the
   * user was at or near during the span
   * @see BrazeEngineIntegration#trackUserJourneySpans(Context)
   */
  public static void trackUserJourneySpans(Context context,
      int numMaxAttachedPlaceEventsPerSpan) {
    // Set max number of attached places to send
    context.getApplicationContext()
        .getSharedPreferences(BrazeEngineIntegration.class.getName(),
            Context.MODE_PRIVATE)
        .edit()
        .putInt(NUM_MAX_ATTACHED_PLACE_EVENTS_PER_SPAN_KEY,
            numMaxAttachedPlaceEventsPerSpan)
        .apply();

    // Start sending user journey spans to Braze
    Log.i(TAG, "Enabling Braze -> Engine span logging");
    BrazeEngineIntegration.trackingSpans = true;
  }

  /**
   * Disables Braze Engine's User Journey Span tracking
   */
  public static void stopTrackingUserJourneySpans() {
    Log.i(TAG, "Disabling Braze -> Engine span logging");
    BrazeEngineIntegration.trackingSpans = false;
  }

  /**
   * @return true if tracking spans is enabled
   */
  public static Boolean isTrackingSpans() {
    return BrazeEngineIntegration.trackingSpans;
  }
}
