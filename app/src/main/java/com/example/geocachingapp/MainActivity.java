package com.example.geocachingapp;

//Standard Android-Imports
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
//ZXING-Imports für den QR-Code Scanner
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
//Mapbox-Imports für die Karte und die aktuelle Position
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
//Standard Java-Imports
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    // region UI-Elemente
    //In dieser Region werden die UI-Elemente aus der activity_main.xml Variablen zugewiesen
    //So können die Elemente hier im Code angesprochen werden
    private FrameLayout dynamicContent;
    private Button button1;
    private Button button2;
    private Button button3;
    private Button bottomButton;
    private MapView mapView;
    // endregion

    // region Globale Variablen
    //Hier werden Globale Variablen festgelegt, also Variablen die vom ganzen Code aus benutzt werden können sollen
    //Die Variablen in Großbuchstaben sind Konstanten, die sich nicht ändern
    //Den PUBLIC_KEY benötigt die Mapbox SDK
    //Für dieses GitHub-Repo habe ich meinen Key hier entfernt
    static final String PUBLIC_KEY = "YOUR_PUBLIC_KEY";
    //Request-Codes identifizieren, welche Erlaubnis angefordert wurde, bzw. welche Datei gescpeichert wird
    static final int REQUEST_CODE_FINE_LOCATION = 0;
    static final int REQUEST_CODE_CAMERA = 2;
    static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_GPX = 4;
    static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PNG = 5;
    static final double RADIUS_EARTH = 6371000;
    //Zwei Zoom-Level für die Karte (einmal beim Start und einmal beim View-wechsel in der App)
    static final double MAPBOX_MAP_ZOOM_LEVEL_1 = 14;
    static final double MAPBOX_MAP_ZOOM_LEVEL_2 = 17;
    static final long MAPBOX_MAP_ANIMATION_DURATION = 3000;
    static final int DEFAULT_INTERVAL_IN_MILLISECONDS=200;
    static final int DEFAULT_MAX_WAIT_TIME=10000;
    static final String [] GEOCACHE_NAMES={"Akkuschrauber","Blatt","Blaulicht","Controller","Haus","Lampe","Schluessel","Shirt","Wolke","Zange"};
    //Legt fest ob Audio-Ausgabe aktiviert sein soll
    private boolean audioON=false;
    // Gibt an ob man in der Karten-/Geocache-/ oder Einstellungs-Ansicht ist
    private String layoutMode = "button1";
    //Gibt man ob man in der Verstecken- oder Suchen-Phase ist
    private boolean gameStatus = false;
    //Gibt an, ob man Caches per Checkbox (="debug") auswählt oder per QR-Code (="qr")
    private String gameMode = "debug";
    //Gibt die minimale Distanz für die Toast- Audio-Ausgabe an auf die man sich im Suchen-Modus einem Cache annähern muss
    private int minDistance=50;
    //Speichert die letzte gemessene Position
    private double currentLatitude=0.0;
    private double currentLongitude=0.0;
    //Speichert für jeden Cache die letzte Position ab
    //So wird nicht jedes mal wenn die Distanz gecheckt wird ein Toast/Audio ausgegeben wenn man näher als die minDistance ist,
    //sondern nur dann wenn die vorherige Distanz nicht auch näher als minDistance war
    private double [] previousDistances = new double[10];
    private LocationEngineCallback<LocationEngineResult> locationEngineCallback;
    public MapboxMap map;
    private LocationEngine locationEngine;
    private TextToSpeech textToSpeech;
    List<Geocache> geocacheList = new ArrayList<>();
    List<Bitmap> bitmapList = new ArrayList<>();

    // endregion

    // region Lifecycle-Methoden
    //Hier werden die Lifecycle-Methoden implementiert
    //Überraschenderweise hat sich bei den Test keine Notwendigkeit gezeigt andere klassische Lifecycle-Methoden wie onResume() zu implementieren
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this,PUBLIC_KEY);
        setContentView(R.layout.activity_main);

        dynamicContent = findViewById(R.id.dynamicContent);
        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        bottomButton = findViewById(R.id.bottomButton);
        //Hier wird festgelegt welche Funktion beim Button-Klick ausgeführt werden soll
        button1.setOnClickListener(view -> onButton1Click());
        button2.setOnClickListener(view -> onButton2Click());
        button3.setOnClickListener(view -> onButton3Click());
        bottomButton.setOnClickListener(view -> onBottomButtonClick());

        initializeGeocaches();
        initializeBitmaps();
        initializeDistances();
        //Erstes Layout darf erst gesetzt werden wenn die Buttons deklariert sind
        setInitialLayout("button1");
        initializeTextToSpeech();
    }
    //Regelt, was gemacht wird, wenn eine Erlaubnis erteilt wurde
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==REQUEST_CODE_FINE_LOCATION){
            if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Standorterlaubnis erteilt!",Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this,"Standorterlaubnis nicht erteilt! So können Sie NICHT spielen!",Toast.LENGTH_LONG).show();}
        }
        if (requestCode==REQUEST_CODE_CAMERA){
            if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Kameraerlaubnis erteilt!",Toast.LENGTH_SHORT).show();
                qrScanFunction(MainActivity.this);
            }else{
                Toast.makeText(this,"Kameraerlaubnis nicht erteilt! So können Sie KEINE QR-Codes scannen!",Toast.LENGTH_LONG).show();}
        }
    }
    //Regelt, was gemacht wird, wenn ein Dateispeicherort ausgewählt wurde, abhängig von der zu speichernden Datei
    //bzw. was gemacht wird, wenn die Kamera für den QR-Code-Scanner geöffnet wird
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE_GPX && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                exportToGPXFile(uri, geocacheList);
            }
        }
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PNG && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                exportToPNGFile(uri);
            }
        }
        if (requestCode == REQUEST_CODE_CAMERA && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String content = new ScanContract().parseResult(resultCode,data).getContents();
                Toast.makeText(this,"\""+content+"\" gescannt.",Toast.LENGTH_SHORT).show();
                qrScanResultFunction(content);
            }
        }
    }
    // endregion

    // region Initialisierungs-Funktionen
    //Diese Funktionen müssen zum Start ausgeführt werden
    //Geocache-Objekte der eignen Geocache-Klasse werden erzeugt mit den Namen der Icons aus der Google material-icons Gallerie
    void initializeGeocaches(){
        for (String geocacheName : GEOCACHE_NAMES) {
            geocacheList.add(new Geocache(geocacheName));
        }
    }
    //Die Icons der Geocaches werden als Bitmap-Objekte in eine Liste aufgenommen
    //so können sie später auf der Karte angezeigt werden
    void initializeBitmaps(){
        Bitmap Akkuschrauber = BitmapFactory.decodeResource(getResources(), R.drawable.akkuschrauber);
        bitmapList.add(Akkuschrauber);
        Bitmap Blatt = BitmapFactory.decodeResource(getResources(), R.drawable.blatt);
        bitmapList.add(Blatt);
        Bitmap Blaulicht = BitmapFactory.decodeResource(getResources(), R.drawable.blaulicht);
        bitmapList.add(Blaulicht);
        Bitmap Controller = BitmapFactory.decodeResource(getResources(), R.drawable.controller);
        bitmapList.add(Controller);
        Bitmap Haus = BitmapFactory.decodeResource(getResources(), R.drawable.haus);
        bitmapList.add(Haus);
        Bitmap Lampe = BitmapFactory.decodeResource(getResources(), R.drawable.lampe);
        bitmapList.add(Lampe);
        Bitmap Schluessel = BitmapFactory.decodeResource(getResources(), R.drawable.schluessel);
        bitmapList.add(Schluessel);
        Bitmap Shirt = BitmapFactory.decodeResource(getResources(), R.drawable.shirt);
        bitmapList.add(Shirt);
        Bitmap Wolke = BitmapFactory.decodeResource(getResources(), R.drawable.wolke);
        bitmapList.add(Wolke);
        Bitmap Zange = BitmapFactory.decodeResource(getResources(), R.drawable.zange);
        bitmapList.add(Zange);
    }
    //Die "default-Distanzen" werden so festgelegt, als wäre man außerhalb des Radius
    //somit ertönt einmal die Audio-Ausgabe/zeigt der Toast, wenn man sich bereits innerhalb des Radius befindet
    void initializeDistances(){
        for (int i = 0; i < 10; i++) {
            previousDistances[i]=minDistance+1;
        }
    }
    //Die Audio-Ausgabe wird initialisiert
    void initializeTextToSpeech(){
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                //Sprache wird auf Deutsch gestellt
                int result = textToSpeech.setLanguage(Locale.GERMAN);
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(MainActivity.this, "Language not supported", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Initialization failed", Toast.LENGTH_SHORT).show();
            }
        });

        //Die App benötigt die nächsten Methoden nicht, die Library verlangt allerdings, dass Sie implementiert werden
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
            }
            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
            @Override
            public void onError(String utteranceId) {
            }
        });
    }
    // endregion

    // region Karten-Funktionen
    //mapFunction überprüft, ob die Standortberechtigung erteilt wurde, falls nicht, wird sie angefragt
    void mapFunction (String layoutBefore){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
            updateLocation(layoutBefore);
            initializeLocationEngine();
        } else{
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE_FINE_LOCATION);
        }
    }
    //"MissingPermission" wird unterdrückt, das die updateLocation() Funktion nur aufgerufen wird, wenn die Standortberechtigung erteilt wurde
    @SuppressLint("MissingPermission")
    void updateLocation(String layoutBefore){
        //Die Karte wird abgefragt und erst wenn sie bereit ist, wird die mächste Funktion (onMapReaddy()) aufgerufen
        mapView.getMapAsync(mapboxMap -> onMapReady(mapboxMap, layoutBefore));
    }
    protected void onMapReady(MapboxMap map,String layoutBefore){
        this.map = map;
        //Der Stil der Karte wird gesetzt und (wie in updateLocation()) wird die nächste Funktion (onMapStyleReady())
        //erst aufgerufen, wenn der Stil bereit ist
        map.setStyle(Style.MAPBOX_STREETS, style -> onMapStyleReady(style,layoutBefore));
    }
    //Der eigentliche Karten-Code wird also erst ausgeführt, wenn überprüft wurde, ob
    //  1. die Standortberechtigung erteilt wurde,
    //  2. die Karte bereit ist und
    //  3. der Kartenstil bereit ist.
    @SuppressLint("MissingPermission")
    protected void onMapStyleReady(Style style, String layoutBefore){
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            //Der locationComponant ist der Kreis der den eigenen Standort anzeigt
            LocationComponent locationComponent = map.getLocationComponent();
            //Dem locationComponant wird der Kartenstil zugewiesen und er wird aktiviert
            locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(this, style).build());
            locationComponent.setLocationComponentEnabled(true);
            //Wird festgelegt, dass die "Kamera" dem eigenen Standort folgen soll
            locationComponent.setCameraMode(CameraMode.TRACKING);
            //Wird die App frisch geöffnet gibt es eine Zoomanimation auf den aktuellen Standpunkt
            //Wird einfach aus anderen Layouts hierhin gewechselt, gibt es keine Animation und der Zoom ist näher
            if (Objects.equals(layoutBefore, "button1")) {
                locationComponent.zoomWhileTracking(MAPBOX_MAP_ZOOM_LEVEL_1, MAPBOX_MAP_ANIMATION_DURATION);
            }
            else{
                locationComponent.zoomWhileTracking(MAPBOX_MAP_ZOOM_LEVEL_2, 0);
            }
            locationComponent.setRenderMode(RenderMode.COMPASS);
            if (locationComponent.getLastKnownLocation()==null){
                Toast.makeText(this,"Auf Standort warten...",Toast.LENGTH_SHORT).show();
            }
            //Wenn man gerade versteckt oder ein Geocache gefunden wurde, soll er auf der Karte eingezeichnet werden
            for (int i = 0; i < 10; i++) {
                Geocache geocache = geocacheList.get(i);
                if ((geocache.hidden &&!gameStatus) || (geocache.hidden &&geocache.found)) {
                    List<Feature> features = new ArrayList<>();
                    //Der Code funktioniert mit einem for-loop, da
                    //  1. die geocacheList mit den GEOCACHE_NAMES initialisiert wurde und
                    //  2. die bitmapList nach der gleichen Reihenfolge initialisiert wurde
                    //so stimmen die Indizes überein
                    features.add(Feature.fromGeometry(Point.fromLngLat(geocache.longitude, geocache.latitude)));
                    style.addImage(geocache.name, bitmapList.get(i));
                    style.addSource(new GeoJsonSource("marker-source" + i, FeatureCollection.fromFeatures(features)));
                    style.addLayer(new SymbolLayer("marker-style-layer" + i, "marker-source" + i)
                            .withProperties(
                                    PropertyFactory.iconAllowOverlap(true),
                                    PropertyFactory.iconIgnorePlacement(true),
                                    PropertyFactory.iconImage(geocache.name),
                                    PropertyFactory.iconOffset(new Float[]{0f, -0f})
                            ));
                }
            }
        }
    }
    //LocationEngine speichert den aktuellen Standort immer ab, wenn er sich ändert
    //LocationEngine kommt auch von der MapboxSDK
    @SuppressLint("MissingPermission")
    private void initializeLocationEngine() {
        if (locationEngine != null){
            return;
        }
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);
        //Hier wird überprüft, ob eine Meldung der LocationEngine kommt
        locationEngineCallback = new LocationEngineCallback<LocationEngineResult>() {
            @Override
            public void onSuccess(LocationEngineResult result) {
                Location location = result.getLastLocation();
                if (location != null) {
                    //Ist die Meldung erfolgreich, wird die Position abgespeichert
                    currentLatitude=location.getLatitude();
                    currentLongitude=location.getLongitude();
                    //Befindet man sich in der Such-Phase, wird die Distanz zu allen versteckten Geocaches überprüft
                    if (gameStatus){
                        checkDistance();
                    }
                }
            }
            //Ist die Meldung nicht erfolgreich, wird nichts getan
            //Diese Methode muss implementiert werden
            @Override
            public void onFailure(@NonNull Exception exception) {
            }
        };
        //Hier wird der locationEngine zugewiesen, wie oft sie überprüfen soll, ob sich der Standort geändert hat
        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();
        locationEngine.requestLocationUpdates(request, locationEngineCallback, getMainLooper());
    }
    //endregion

    // region QR-Code-Funktionen
    //Wird der QR-Scan Button gedrückt, wird erst überprüft, ob die Kamera-Berechtigung erteilt wurde
    void qrScanFunction(Activity activity){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED){
            //Ist die Kameraberechtigung erteilt, werden Scanoptionen erstellt
            ScanOptions scanOptions = new ScanOptions();
            scanOptions.setOrientationLocked(false);
            scanOptions.setPrompt("Scan QR-Code");
            scanOptions.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            //Nun wird ein Intent erstellt und gewartet, bis eine Rückmeldung kommt (siehe onActivityResult())
            //Ist die Rückmeldung Positiv, wird qrScanResultFunction() ausgeführt
            Intent intent = scanOptions.createScanIntent(MainActivity.this);
            activity.startActivityForResult(intent, REQUEST_CODE_CAMERA);
        } else{
            //Ist die Kameraberechtigung nicht erteilt, wird sie angefragt
            requestPermissions(new String[]{Manifest.permission.CAMERA},REQUEST_CODE_CAMERA);
        }
    }
    //Ist das ActivityResult positiv, wird der Text des QR-Codes als String an qrScanResultFunction() übergeben
    void qrScanResultFunction(String result){
        for (Geocache geocache : geocacheList) {
            //Hier wird der geocache mit dem Übereinstimmenden Namen herausgepickt
            if (Objects.equals(geocache.name, result)){
                //Hier wird überprüft, ob wir in der Suchen-Phase sind
                if (gameStatus){
                    //Sind wir in der Suchen-Phase und der gescannte Geocache wurde auch versteckt im aktuellen Spiel
                    //dann wird er als gefunden markiert
                    if (geocache.hidden){
                        geocache.setFound(true);
                    }
                }else{
                    //Befinden wir uns in der Verstecken-Phase wird der Geocache als versteckt markiert
                    geocache.setHidden(true);
                }
                //Das Layout wird neu geladen, dass sich die "checks" der CheckBoxen aktualisieren
                setSecondLayout();
                //Es wird überprüft, ob das Spiel gewonnen wurde
                checkWin("button2");
            }
        }
    }
    // endregion

    // region Sprachausgabe-Funktionen
    //Die Funktion, die ein String als Audio vorliest
    public void readOutLoud(String word) {
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "messageId");
        textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, "messageId");
    }
    // endregion

    // region Check-Funktionen
    //checkWin() überprüft, ob das Spiel gewonnen wurde, also alle versteckten Geocaches gefunden wurden
    //Übergabeparameter activation gibt an, wie die Funktion aufgerufen wurde
    void checkWin(String activation){
        int hidden =0;
        int found =0;
        for (Geocache geocache : geocacheList) {
            if (geocache.hidden){
                hidden +=1;
            }
            if (geocache.found){
                found +=1;
            }
        }
        //Ist die Anzahl der gefundenen Geocaches gleich der Anzahl der versteckten, ist das Spiel gewonnen
        if (hidden==found){
            Toast.makeText(this,"Glückwunsch! Alle Caches gefunden.",Toast.LENGTH_LONG).show();
        }else if (Objects.equals(activation, "bottomButton")){
            //Wenn die Funktion aufgerufen wurde, da das Spiel beendet wurde und nicht, weil ein neuer Geocache gefunden wurde,
            //dann kann auch eine Niederlage angezeigt werden
            //(Wenn das Spiel nicht zu Ende ist kann es auch nicht verloren sein)
            Toast.makeText(this,"Schade! Nur "+ found +" von "+ hidden +" Caches gefunden.",Toast.LENGTH_LONG).show();
        }
    }
    //checkWin() überprüft die Distanz der aktuellen Position zu allen Geocaches
    void checkDistance(){
        for (int i = 0; i < geocacheList.size(); i++) {
            Geocache geocache = geocacheList.get(i);
            if (geocache.longitude==null || geocache.latitude == null){
                continue;
            }
            double calcDistance=geocache.returnDistance(currentLatitude,currentLongitude);
            //Nur, wenn die Distanz des Geocaches kleiner als die minDistance ist UND sie nicht bereits zuvor kleiner als
            //minDistance was, wird sie angezeigt
            if (calcDistance<=minDistance && previousDistances[i]>minDistance){
                //Sind Audioausgaben aktiviert, wird der Name des Geocaches vorgelesen
                if (audioON){
                    readOutLoud(geocache.name);
                }
                //Egal ob Audioausgabe aktiviert ist oder nicht, wird die Distanz angezeigt
                Toast.makeText(this,"Sie sind auf "+ calcDistance +"m an "+geocache.name+".",Toast.LENGTH_SHORT).show();
            }
            //Egal was die if-Bedingngen in dieser Funktion ergeben, die previousDistance wird immer aktualisiert
            previousDistances[i]=calcDistance;
        }
    }
    // endregion

    // region Export-Funktionen
    void exportToGPX(Activity activity) {
        //Öffnet die Auswahl für den Speicherort der GPX-Datei
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/gpx");
        intent.putExtra(Intent.EXTRA_TITLE, "GeocachingData.gpx");
        //So wird wieder gewartet, bis eine Rückmeldung aus dem Speicherortauswahl-Dialog zurückkommt
        activity.startActivityForResult(intent, REQUEST_CODE_WRITE_EXTERNAL_STORAGE_GPX);
    }
    //Funktioniert analog zu exportToGPX()
    void exportToPNG(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/png");
        intent.putExtra(Intent.EXTRA_TITLE, "GeocachingApp_QR-Codes.png");
        activity.startActivityForResult(intent, REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PNG);
    }
    //War der Speicherortauswahl-Dialog erfolgreich, werde die QR-Codes als PNG abgespeichert
    //In der Uri ist der Speicherpfad enthalten
    private void exportToPNGFile(Uri uri) {
        try {
            //Die QR-Codes befinden sich im res/drawable Ordner
            @SuppressLint("ResourceType") InputStream inputStream = getResources().openRawResource(R.drawable.qrcodes);
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (inputStream != null && outputStream != null) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                inputStream.close();
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //War der Speicherortauswahl-Dialog erfolgreich, werden die Geocaches als GPX-Datei abgespeichert
    void exportToGPXFile(Uri uri, List<Geocache> geocaches) {
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                //GPX-header wird geschrieben
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                writer.write("<gpx version=\"1.1\" creator=\"GeocachingApp\">\n");
                //Es wird über die Geocaches iteriert und sie werden in die Datei geschrieben
                for (Geocache geocache : geocaches) {
                    writer.write("  <wpt lat=\"" + geocache.latitude + "\" lon=\"" + geocache.longitude + "\">\n");
                    writer.write("    <name>" + geocache.name + "</name>\n");
                    //Die Höhe wird nur geschrieben, wenn sie existiert (tut sie nicht)
                    //GPS-Höhen (v.a. vom Handy) sind *viel* zu ungenau, als dass Sie von Hilfe sein könnten
                    if (geocache.altitude != null) {
                        writer.write("    <ele>" + geocache.altitude + "</ele>\n");
                    }
                    //Datums-Format, damit das Date-Object korrekt zu String gecastet werden kann
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                    //Überprüfung, ob der Cache gefunden wurde
                    if (geocache.found) {
                        writer.write("    <desc>" + "gefunden" + "</desc>\n");
                        String formattedTime = dateFormat.format(geocache.foundTime);
                        writer.write("    <time>" + formattedTime + "</time>\n");
                    } else {
                        writer.write("    <desc>" + "nicht gefunden" + "</desc>\n");
                    }
                    writer.write("  </wpt>\n");
                }
                //GPX-Footer wird geschrieben
                writer.write("</gpx>");
                //Der Writer wird geschlossen
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // endregion

    // region Layout
    //Die Layout werden dynamisch (nicht statisch in der activity_main.xml gesetzt
    //außer die oberen drei Butttons und der untere Button
    //Die oberen drei Buttons setzten die verschiedenen Layouts
    protected void onButton1Click(){
        setInitialLayout(layoutMode);
        if (Objects.equals(layoutMode, "button3")){
            //Das Funktioniert, da ich nur in der Verstecken-Phase in die Einstellungen gehen kann
            bottomButton.setText("Spiel starten");
        }
        layoutMode = "button1";
        bottomButton.setEnabled(true);
    }
    protected void onButton2Click(){
        setSecondLayout();
        if (Objects.equals(layoutMode, "button3")){
            bottomButton.setText("Spiel starten");
        }
        layoutMode = "button2";
        bottomButton.setEnabled(true);
    }
    protected void onButton3Click(){
        setThirdLayout();
        layoutMode = "button3";
        bottomButton.setEnabled(false);
        bottomButton.setText("");
    }
    //Der bottomButton ändert den Spiel-Status (verstecken / suchen)
    protected void onBottomButtonClick(){
        //Wenn das Spiel beendet wird
        if (gameStatus){
            gameStatus = false;
            button3.setEnabled(true);
            bottomButton.setText("Spiel starten");
            checkWin("bottomButton");
        //Wenn das Spiel gestartet wird
        } else{
            gameStatus = true;
            //Man kann nicht mehr in die Einstellungen
            button3.setEnabled(false);
            bottomButton.setText("Spiel stoppen");
            //Distanzen werden frisch initialisiert und es wird zurückgesetzt, welcher Geocache gefunden wurde
            initializeDistances();
            for (Geocache geocache : geocacheList) {
                geocache.setFound(false);
            }
        }
        //Abhängig vom Layout wird das aktuelle Layout neu geladen
        if (Objects.equals(layoutMode, "button1")){
            setInitialLayout("button2");
        } else if (Objects.equals(layoutMode, "button2")){
            setSecondLayout();
        }
    }
    //Karten-Layout
    private void setInitialLayout(String layoutBefore) {
        //Vorheriges Layout wird entfernt
        dynamicContent.removeAllViews();
        //Karten-Layout wird hinzugefügt
        mapView=new MapView(this);
        dynamicContent.addView(mapView);
        mapFunction(layoutBefore);

    }
    //Geocache-Layout
    private void setSecondLayout() {
        //Vorheriges Layout wird entfernt
        dynamicContent.removeAllViews();
        //Vertikales Layout
        LinearLayout CacheLayout = new LinearLayout(this);
        CacheLayout.setOrientation(LinearLayout.VERTICAL);
        //Überschrift Scroll-View versteckte Geocaches
        TextView topHeader = new TextView(this);
        topHeader.setText("Versteckte Geocaches");
        CacheLayout.addView(topHeader);
        //Scroll-View versteckte Geocaches
        ScrollView topScrollView = new ScrollView(this);
        LinearLayout topLinearLayout = new LinearLayout(this);
        topLinearLayout.setOrientation(LinearLayout.VERTICAL);
        //Die Checkboxen der zu versteckenden Geocaches werden initialisiert
        for (Geocache geocache : geocacheList) {
            CheckBox checkBox = new CheckBox(this);
            //Is der Geocache als verstckt abgespeichert, wird nach seinem Namen seine Position angezeigt
            //Die aktuelle Position wird ja nicht in Koordinaten angezeigt, drumm macht es nichts,
            //wenn man die Koordinaten in der Suchen-Phase sieht
            //Es hilft einfach weiter, sie von unversteckten zu unterscheiden
            if (geocache.latitude==null || geocache.longitude==null){
                checkBox.setText(geocache.name);
            } else{
                checkBox.setText(geocache.name + " ( "+ geocache.latitude +"  |  "+ geocache.longitude +" )");
            }
            //Ein versteckter Geocache wird mit Hacken makiert
            checkBox.setChecked(geocache.hidden);
            //Man muss in der Versteck-Phase im debug-Modus zu sein, um die Checkboxen auszuwählen
            checkBox.setClickable(Objects.equals(gameMode, "debug")&& !gameStatus);
            //Hier wird überprüft, ob die Checkbox angeklickt wird
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                    @Override
                                                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                        //Je nachdem, ob die Checkbox schon aktivierd war (also der Geocache schon versteckt/nicht versteckt war)
                                                        //wird der Geocache als das Gegenteil markiert
                                                        if (isChecked) {
                                                            for (Geocache geocache2 : geocacheList){
                                                                if (Objects.equals(geocache.name, geocache2.name)){
                                                                    geocache2.setHidden(true);
                                                                    //Layout wird neu geladen um die Häcken zu aktualisiernen
                                                                    setSecondLayout();
                                                                }
                                                            }
                                                        } else {
                                                            for (Geocache geocache2 : geocacheList){
                                                                if (Objects.equals(geocache.name, geocache2.name)){
                                                                    geocache2.setHidden(false);
                                                                    setSecondLayout();
                                                                }
                                                            }
                                                        }
                                                    }
                                                });
            topLinearLayout.addView(checkBox);
        }
        //Hier geschieht praktisch einfach nochmal das gleiche, nur für die zu findeneden, statt die zu versteckenden Geocaches
        //Allerdings werden im unteren Scrollview nur die Geocaches angezeigt, die oben angeklickt (also versteckt) wurden
        //Man soll ja nur die Geocaches suchen könne, die auch versteckt wurden
        topScrollView.addView(topLinearLayout);
        LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                45
        );
        CacheLayout.addView(topScrollView, topParams);
        TextView bottomHeader = new TextView(this);
        bottomHeader.setText("Gefundene Geocaches");
        CacheLayout.addView(bottomHeader);
        ScrollView bottomScrollView = new ScrollView(this);
        LinearLayout bottomLinearLayout = new LinearLayout(this);
        bottomLinearLayout.setOrientation(LinearLayout.VERTICAL);
        for (Geocache geocache : geocacheList){
            if (geocache.hidden){
                CheckBox checkBox = new CheckBox(this);
                if (geocache.latitude==null || geocache.longitude==null){
                    checkBox.setText(geocache.name);
                } else{
                    checkBox.setText(geocache.name + " ( "+ geocache.latitude +"  |  "+ geocache.longitude +" )");
                }
                checkBox.setChecked(geocache.found);
                checkBox.setClickable(Objects.equals(gameMode, "debug")&& gameStatus);
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            for (Geocache geocache2 : geocacheList){
                                if (Objects.equals(geocache.name, geocache2.name)){
                                    geocache2.setFound(true);
                                    setSecondLayout();
                                    //Wurde ein Geocache gefunden, wird überprüft, ob das Spiel gewonnen wurde
                                    checkWin("box");
                                }
                            }
                        } else {
                            for (Geocache geocache2 : geocacheList){
                                if (Objects.equals(geocache.name, geocache2.name)){
                                    geocache2.setFound(false);
                                    setSecondLayout();
                                }
                            }
                        }
                    }
                });
                bottomLinearLayout.addView(checkBox);
            }
        }
        bottomScrollView.addView(bottomLinearLayout);
        LinearLayout.LayoutParams bottomParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                45
        );
        CacheLayout.addView(bottomScrollView, bottomParams);
        //Der Button zum Aktivieren des QR-Code-Scans
        Button qrButton = new Button(this);
        qrButton.setText("Scan QR Code");
        //Die Funktion, die ausgeführt wird, wenn der qrButton gedrückt wird
        qrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                qrScanFunction(MainActivity.this);
            }
        });
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                10
        );
        CacheLayout.addView(qrButton, buttonParams);
        //QR-Button ist im debug-Modus nicht aktiviert
        if (Objects.equals(gameMode, "debug")){
            qrButton.setEnabled(false);
        }
        dynamicContent.addView(CacheLayout);
    }
    //Einstellungen-Layout
    private void setThirdLayout() {
        //Vorheriges Layout wird entfernt
        dynamicContent.removeAllViews();
        //Vertikales Layout
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        //Erste Zeile mit einem Switch und Text auf beiden Seiten
        //Schaltet um zwischen debug- und QR-Scan-Modus
        LinearLayout firstEntry = createSwitchEntry("Debug-Modus", "QR-Code-Modus");
        linearLayout.addView(firstEntry);
        //Zweite Zeile mit einem Switch und Text auf beiden Seiten
        //Entscheidet, ob Audioausgabe aktiviert ist
        LinearLayout secondEntry = createSwitchEntry("Nur Textausgabe", "Sprachausgabe");
        linearLayout.addView(secondEntry);
        //Dritte Zeile setzt die minDistance, die Angibt, ab welchem Radius die aktuelle Entfernung zu einem Geocache angezeigt
        //bzw. als Audio ausgegeben werden soll
        EditText integerInput = new EditText(this);
        integerInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        integerInput.setHint("Minimum Radius eingeben");
        linearLayout.addView(integerInput);
        //Diese Funktion reagiert, wenn die Eingabe für minDistance geändert wurde
        integerInput.addTextChangedListener(new TextWatcher() {
            //Auch wenn nur die afterTextChanged()-Methode benötigt wird, müssen die anderen Metoden dennoch implementiert werden
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    minDistance = Integer.parseInt(editable.toString());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });
        //In der vierten Zeile ist der Button zum Abspeichern der GPX mit den Geocaches
        Button GPXbutton = new Button(this);
        GPXbutton.setText("GPX Export");
        linearLayout.addView(GPXbutton);
        GPXbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exportToGPX(MainActivity.this);
            }
        });
        //In der fünften Zeile ist der Button zum Abspeichern der QR-Codes als PNG
        Button PNGbutton = new Button(this);
        PNGbutton.setText("Export QR-Codes PNG");
        linearLayout.addView(PNGbutton);
        PNGbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exportToPNG(MainActivity.this);
            }
        });
        dynamicContent.addView(linearLayout);
    }
    //Die Funktion, die die Switches mit dem Text links und rechts für das Einstellungslayout kreiert
    private LinearLayout createSwitchEntry(String leftText,String rightText) {
        LinearLayout entryLayout = new LinearLayout(this);
        entryLayout.setOrientation(LinearLayout.HORIZONTAL);
        entryLayout.setGravity(Gravity.CENTER);
        //Linker text
        TextView leftTextView = new TextView(this);
        leftTextView.setText(leftText);
        entryLayout.addView(leftTextView);
        //Switch in der Mitte
        Switch switchToggle = new Switch(this);
        if (Objects.equals(leftText, "Debug-Modus")){
            switchToggle.setChecked(Objects.equals(gameMode, "qr"));
        }else if(Objects.equals(leftText, "Nur Textausgabe")){
            switchToggle.setChecked(audioON);
        }
        entryLayout.addView(switchToggle);
        //Rechter text
        TextView rightTextView = new TextView(this);
        rightTextView.setText(rightText);
        entryLayout.addView(rightTextView);
        //Es wird überprüft, ob der Switch betätigt wurde
        switchToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //Abhängig davon, welcher Switch hier kreiert wird, wird die entsprechende Variable manipuliert
                //Abhängig vom Zustand des Switches, wird der Wert für diese Varaiable gesetzt
                if (isChecked) {
                    if (Objects.equals(leftText, "Debug-Modus")){
                        gameMode="qr";
                    }else if(Objects.equals(leftText, "Nur Textausgabe")){
                        audioON=true;
                    }
                } else {
                    if (Objects.equals(leftText, "Debug-Modus")){
                        gameMode="debug";
                    }else if(Objects.equals(leftText, "Nur Textausgabe")){
                        audioON=false;
                    }
                }
            }
        });
        return entryLayout;
    }
    // endregion

    // region Klassen
    //Eigene Klasse "Geocache"
    public class Geocache {
        //Die Attribute sollten eigentlich private sein
        //z.B. kann man so found zu true setzen, ohne ein foundDate festzulegen, das kann zu Fehlern führen
        //Allerdings überwiegt bei solch einem geringen Ausmaß des Projekts die Einfachheit gegenüber der Sicherheit
        public String name;
        public Double latitude;
        public Double longitude;
        public Double altitude;
        public boolean hidden;
        public boolean found;
        public Date foundTime;
        //Voller Konstruktor
        public Geocache(String name, double latitude, double longitude, double altitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
            this.name = name;
            this.found = false;
            this.hidden = false;
            this.foundTime = null;
        }
        //Konstruktor ohne Höhe
        //Wie bereits erwähnt, Smartphone-GPS-Höhe ist sehr ungenau, es ist sinvoller nur mit Lageinformation zu arbeiten
        public Geocache(String name, double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = null;
            this.name = name;
            this.found = false;
            this.hidden = false;
            this.foundTime = null;
        }
        //Konstruktor nur mit Name
        //Dieser wird tatsächlich verwendet, nämlich wenn die geocacheList initialisiert wird
        public Geocache(String name) {
            this.latitude = null;
            this.longitude = null;
            this.altitude = null;
            this.name = name;
            this.found = false;
            this.hidden = false;
            this.foundTime = null;
        }
        //Berechnet über die Haversine-Formel den Abstand zu einem anderen Punkt
        public double returnDistance(double otherLatitude, double otherLongitude) {
            double dLat = Math.toRadians(otherLatitude) - Math.toRadians(latitude);
            double dLon = Math.toRadians(otherLongitude) - Math.toRadians(longitude);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(otherLatitude)) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return Math.round(RADIUS_EARTH * c*100)/100;
        }
        //Die nächsten beiden Methoden sind notwendig, dass found und hidden *sicher* gesetzt werden
        //So entstehen keine null-Exceptions
        public void setFound(boolean found) {
            this.found = found;
            if (found) {
                this.foundTime = new Date();
            } else {
                this.foundTime = null;
            }
        }
        public void setHidden(boolean hidden){
            this.hidden=hidden;
            if(hidden){
                this.longitude=currentLongitude;
                this.latitude=currentLatitude;
            } else{
                this.latitude=null;
                this.longitude=null;
            }
        }
    }
    // endregion
}
