package ca.cmpt276.iteration1.activities;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.Executors;

import ca.cmpt276.iteration1.R;
import ca.cmpt276.iteration1.adapters.PlayerScoreInputRecyclerViewAdapter;
import ca.cmpt276.iteration1.databinding.ActivityMainBinding;
import ca.cmpt276.iteration1.interfaces.PlayerScoreInputRecyclerViewInterface;
import ca.cmpt276.iteration1.model.GameManager;
import ca.cmpt276.iteration1.model.GameType;
import ca.cmpt276.iteration1.model.PlayedGame;
import ca.cmpt276.iteration1.model.PlayerScoreInput;

/**
* Activity to allow user input the game information by choosing the difficulty, number of player, and each player's score
* */
public class GamePlayActivity extends AppCompatActivity implements PlayerScoreInputRecyclerViewInterface {

    private final int GAME_PLAYED_POSITION_NON_EXISTENT = -1;
    private int gamePlayedPosition;

    private boolean editGameActivity = false;
    private int originalPlayerAmount;

    private boolean difficultySelected = false;
    private boolean playersSelected = false;
    private boolean gameCompleted = false;

    private GameManager gameManager;
    private String gameTypeString;
    private GameType gameType;
    private PlayedGame playedGame;

    private String difficulty;
    private int playerAmount;
    private int totalScore;
    private String takePhoto;

    private ArrayList<Integer> playerScores;

    private EditText etPlayerAmount;
    private RecyclerView rvPlayerScoreInputs;
    private PlayerScoreInputRecyclerViewAdapter recyclerViewAdapter;

    private ActivityMainBinding viewBinding;
    private ImageCapture imageCapture = null;
    private ExecutorService cameraExecutor;

    // If a context and gameType are given, we are creating a new game
    public static Intent makeIntent(Context context, String gameTypeString){
        Intent intent = new Intent(context, GamePlayActivity.class);
        intent.putExtra("GameTypeString", gameTypeString);
        return intent;
    }

    // If a context and position are given, we are editing an existing game
    public static Intent makeIntent(Context context, String gameTypeString, int position){
        Intent intent = new Intent(context, GamePlayActivity.class);
        intent.putExtra("GameTypeString", gameTypeString);
        intent.putExtra("GamePlayedPosition", position);
        return intent;
    }

    private void extractIntentExtras(){
        Intent intent = getIntent();

        gameTypeString = intent.getStringExtra("GameTypeString");
        gameType = gameManager.getGameTypeFromString(gameTypeString);

        gamePlayedPosition = intent.getIntExtra("GamePlayedPosition", GAME_PLAYED_POSITION_NON_EXISTENT);
        if (gamePlayedPosition == GAME_PLAYED_POSITION_NON_EXISTENT){
            // Creating a new game if this condition is true
            return;
        }

        // If a position exit, we are editing an existing game
        // Set up the screen to display info
        setEditGameInfo();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(R.layout.activity_game_play);

        gameManager = GameManager.getInstance();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.create_new_game);

        if(!allPermissionGranted()){
            ActivityCompat.requestPermissions(GamePlayActivity.this, Configuration.REQUIRED_PERMISSION, Configuration.REQUEST_CODE_PERMISSION);
        }
        extractIntentExtras();
        setDifficultyButtons();
        setPhotoOptionsButtons();

        if (editGameActivity == true && difficultySelected == true){
            actionBar.setTitle(R.string.edit_game);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate menu
        getMenuInflater().inflate(R.menu.menu_add_type_appbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case (R.id.btnSave): {
                try {
                    if (difficultySelected == false || playersSelected == false || gameCompleted == false){
                        Toast.makeText(GamePlayActivity.this, R.string.require_user_to_fill_all_field, Toast.LENGTH_SHORT).show();
                        throw new Exception(String.valueOf(R.string.require_user_to_fill_all_field));
                    }

                    // Creating a new game
                    if (gamePlayedPosition == GAME_PLAYED_POSITION_NON_EXISTENT){
                        Toast.makeText(GamePlayActivity.this, R.string.game_created, Toast.LENGTH_SHORT).show();
                        saveNewGame();
                    }
                    // Editing an existing game
                    else {
                        Toast.makeText(GamePlayActivity.this, R.string.game_changes_saved, Toast.LENGTH_SHORT).show();
                        saveExistingGame();
                    }

                    // Animate the star to signify the achievement
                    ImageView starImage = findViewById(R.id.ivGameSaveAnimation);
                    Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);
                    starImage.startAnimation(animation);
                    starImage.setVisibility(View.VISIBLE);

                    // Store a reference to the activity so we can end the activity after an animation finishes
                    Activity thisActivity = this;

                    // End the activity after the animation finished https://stackoverflow.com/questions/7606498/end-animation-event-android
                    animation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            rvPlayerScoreInputs.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            //taken from https://stackoverflow.com/questions/37248300/how-to-finish-specific-activities-not-all-activities
                            Intent intent = new Intent(thisActivity, GamePlayedListActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {}
                    });

                    // Play a sound
                    MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.achievement_jingle);
                    mediaPlayer.start(); // no need to call prepare(); create() does that for you

                    return false;
                }
                catch (Exception e){
                    return false;
                }
            }
            case (android.R.id.home): {
                finish();
            }
        }

        return true;
    }

    void saveNewGame(){
        updatePlayerScores();

        int achievementIndex = gameType.getAchievementIndex(totalScore, playerAmount, difficulty);
        LocalDateTime datePlayed = LocalDateTime.now();
        PlayedGame currentGame = new PlayedGame(gameTypeString, playerAmount, totalScore, achievementIndex, difficulty, playerScores, datePlayed);
        gameManager.addPlayedGame(currentGame);
    }

    void saveExistingGame(){
        updatePlayerScores();

        int achievementIndex = gameType.getAchievementIndex(totalScore, playerAmount, difficulty);
        playedGame.editPlayedGame(playerAmount, totalScore, achievementIndex, difficulty, playerScores);
    }

    private void setDifficultyButtons(){
        Button btnDifficultyEasy = findViewById(R.id.btnDifficultyEasy);
        Button btnDifficultyNormal = findViewById(R.id.btnDifficultyNormal);
        Button btnDifficultyHard = findViewById(R.id.btnDifficultyHard);

        btnDifficultyEasy.setTag("Easy");
        btnDifficultyNormal.setTag("Normal");
        btnDifficultyHard.setTag("Hard");

        ArrayList<Button> difficultyButtons = new ArrayList<>();
        difficultyButtons.add(btnDifficultyEasy);
        difficultyButtons.add(btnDifficultyNormal);
        difficultyButtons.add(btnDifficultyHard);

        if (editGameActivity == true){
            highlightSelectedButton(difficulty, difficultyButtons);
        }

        // Choosing player count is hidden by default as a user needs to select a difficulty first
        // If any of these buttons are pressed, enable player count input
        btnDifficultyEasy.setOnClickListener(view -> {
            highlightSelectedButton("Easy", difficultyButtons);

            difficulty = "Easy";
            difficultySelected = true;
            enableHiddenElements();
            updateScoreTextView();
        });
        btnDifficultyNormal.setOnClickListener(view -> {
            highlightSelectedButton("Normal", difficultyButtons);

            difficulty = "Normal";
            difficultySelected = true;
            enableHiddenElements();
            updateScoreTextView();
        });
        btnDifficultyHard.setOnClickListener(view -> {
            highlightSelectedButton("Hard", difficultyButtons);

            difficulty = "Hard";
            difficultySelected = true;
            enableHiddenElements();
            updateScoreTextView();
        });
    }

    //this function is to set up the yes no button that asking whether the user willing to take a photo to save for the game play
    private void setPhotoOptionsButtons() {
        Button btnYes = findViewById(R.id.btnYesToTakePhoto);
        Button btnNo = findViewById(R.id.btnNoToTakePhoto);
        ArrayList<Button> takePhotoButton = new ArrayList<>();

        btnYes.setTag("Yes");
        btnNo.setTag("No");

        takePhotoButton.add(btnYes);
        takePhotoButton.add(btnNo);

        if(editGameActivity){
            highlightSelectedButton(takePhoto, takePhotoButton);
        }

        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                highlightSelectedButton("Yes", takePhotoButton);
                takePhoto = "Yes";
                /*startCamera();
                Button imageCaptureButton = findViewById(R.id.image_capture_button);
                imageCaptureButton.setVisibility(View.VISIBLE);
                imageCaptureButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        takePhoto();
                    }
                });
                cameraExecutor = Executors.newSingleThreadExecutor();*/

                /*Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                ActivityResultLauncher<Intent> startActivityIntent = null;
                startActivityIntent.launch(cameraIntent);

                startActivityIntent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        Intent i = result.getData();
                        Bundle extras = i.getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");

                        ImageView iv = findViewById(R.id.ivGamePhoto);
                        iv.setImageBitmap(imageBitmap);

                        grabImage(imageBitmap);
                    }
                });*/
            }
        });
        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                highlightSelectedButton("No", takePhotoButton);
                takePhoto = "No";
            }
        });
    }

    //request user permission
    public boolean allPermissionGranted(){
        for(String permission : GamePlayActivity.Configuration.REQUIRED_PERMISSION){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == Configuration.REQUEST_CODE_PERMISSION){
            if(allPermissionGranted()){
                startCamera();
            }
            else{
                Toast.makeText(this, "User decline to give access to the application", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                //Used to bind the lifecycle of cameras to the lifecycle owner
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                //Preview
                PreviewView viewFinder = findViewById(R.id.viewFinder);
                viewFinder.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder().build();
                viewFinder.setVisibility(View.VISIBLE);

                // Select back camera as a default
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                //Unbind use cases before rebinding
                cameraProvider.unbindAll();

                //Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                Log.e(Configuration.TAG, "Use case binding failed" + e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto(){
        if(imageCapture == null){
            return;
        }
        //Create time stamped name and MediaStore entry.
        String name = new SimpleDateFormat(Configuration.FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");
        }

        //Create output options object which contains file + metadata
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build();

        //Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback(){
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                String msg = "Photo capture succeeded: " + outputFileResults.getSavedUri();
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                Log.d(Configuration.TAG, msg);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(Configuration.TAG, "Photo capture failed: " + exception.getMessage());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        cameraExecutor.shutdown();
    }

    static class Configuration{
        public static final String TAG = "CameraxBasic";
        public static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm";
        public static final int REQUEST_CODE_PERMISSION = 10;
        public static final String[] REQUIRED_PERMISSION = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P ?
                new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}:
                new String[]{Manifest.permission.CAMERA};
    }


    private void grabImage(Bitmap imageBitmap) {
        //Create a folder for storing images of each game play
        File directory = new File(Environment.getExternalStorageDirectory(), "gamePlayPhotos");
        if(!directory.exists()){
            directory.mkdirs();
        }

        //Save the image to jpeg
        File imageFile = new File(directory, System.currentTimeMillis() + ".jpg");
        OutputStream outputStream;
        try{
            //Create the output stream
            outputStream = new FileOutputStream(imageFile);

            //Compress the bitmap
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

            //Close the output stream
            outputStream.flush();
            outputStream.close();
        }
        catch(Exception e){
            Toast.makeText(this, "Couldn't save image! Permission required.", Toast.LENGTH_SHORT).show();
        }
    }

    private void highlightSelectedButton(String selectedButtonTag, ArrayList<Button> buttons){
        for (Button difficultyButton : buttons){
            if (difficultyButton.getTag().equals(selectedButtonTag)){
                difficultyButton.setBackgroundColor(Color.BLACK);
            }
            else {
                difficultyButton.setBackgroundColor(getColor(R.color.purple_500));
            }
        }
    }

    private void enableHiddenElements(){
        // The player amount edittext and recyclerview containing cards to fill in player scores are hidden by default
        // The user must select a difficulty first in order to select amount of players and input scores
        TextView tvChoosePlayerAmount = findViewById(R.id.tvChoosePlayerAmount);
        ScrollView svPhotoTakingOptions = findViewById(R.id.svPhotoOption);
        etPlayerAmount = findViewById(R.id.etPlayerCount);
        rvPlayerScoreInputs = findViewById(R.id.rvPlayerScoreInputs);

        tvChoosePlayerAmount.setVisibility(View.VISIBLE);
        etPlayerAmount.setVisibility(View.VISIBLE);
        etPlayerAmount.addTextChangedListener(playerCountInputWatcher);
        rvPlayerScoreInputs.setVisibility(View.VISIBLE);
        svPhotoTakingOptions.setVisibility(View.VISIBLE);
    }

    private final TextWatcher playerCountInputWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            try {
                playerAmount = Integer.parseInt(etPlayerAmount.getText().toString());

                // When the user chagnes the amount of players, we want to reset the adapter and textview for total score
                // This prevents any old data from persisting and being carried over - basically gives the user a fresh start!
                recyclerViewAdapter = null;
                TextView tvScoreWithAchievementLevel = findViewById(R.id.tvScoreWithAchievementLevel);
                tvScoreWithAchievementLevel.setText(R.string.waiting_player_score_input);

                playersSelected = true;
                setupGameInfoModels();

                updatePlayerScores();
            }
            catch (NumberFormatException numberFormatException){
                playersSelected = false;
                Log.i("Undefined Player Amount", getString(R.string.waiting_user_new_input));
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    private void setupGameInfoModels() {
        // Nothing too complicated, we're just giving each "player score input card" an id ranging from 0 to playerAmount
        // This will help is keep track of which cards have a score inputted or not later on

        ArrayList<PlayerScoreInput> playerScoreInputs = new ArrayList<>();
        // If we are creating a new game, we do not need to pull existing scores from the existing game
        if (editGameActivity == false){
            for (int i = 0; i < playerAmount; i++){
                playerScoreInputs.add(new PlayerScoreInput(i));
            }

            setupRecyclerView(playerScoreInputs);
        }
        if (editGameActivity == true){
            for (int i = 0; i < playerAmount; i++){
                if (i >= playerScores.size()){
                    playerScoreInputs.add(new PlayerScoreInput(i));
                }
                else {
                    playerScoreInputs.add(new PlayerScoreInput(i, playerScores.get(i)));
                }
            }

            setupRecyclerView(playerScoreInputs);
        }
    }

    private void setEditGameInfo(){
        enableHiddenElements();

        editGameActivity = true;
        difficultySelected = true;
        playersSelected = true;
        gameCompleted = true;

        gameType = gameManager.getGameTypeFromString(gameTypeString);
        playedGame = gameManager.getSpecificPlayedGames(gameTypeString).get(gamePlayedPosition);

        difficulty = playedGame.getDifficulty();
        playerAmount = playedGame.getNumberOfPlayers();
        totalScore = playedGame.getTotalScore();
        playerScores = playedGame.getPlayerScores();

        originalPlayerAmount = playerAmount;

        EditText etPlayerCount = findViewById(R.id.etPlayerCount);
        etPlayerCount.setText(String.valueOf(playerAmount));

        updateScoreTextView();
        setupGameInfoModels();
    }

    private void setupRecyclerView(ArrayList<PlayerScoreInput> playerScoreInputs){
        RecyclerView recyclerView = findViewById(R.id.rvPlayerScoreInputs);
        recyclerViewAdapter = new PlayerScoreInputRecyclerViewAdapter(GamePlayActivity.this, playerScoreInputs, editGameActivity, GamePlayActivity.this);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(GamePlayActivity.this));
    }


    @Override
    public void checkAllPlayerScoreInputs() {
        updatePlayerScores();
        updateScoreTextView();
    }

    private void updatePlayerScores() {
        ArrayList<Integer> playerScores = recyclerViewAdapter.getScores();

        // One or more of the scores must be invalid, the game hasn't been completed, don't recalculate!
        if (playerScores == null) {
            gameCompleted = false;
            return;
        }

        setTotalGameScore(playerScores);
    }

    private void setTotalGameScore(ArrayList<Integer> playerScores){
        totalScore = 0;
        for (int score : playerScores){
            totalScore += score;
        }
        this.playerScores = playerScores;
        updateScoreTextView();

        gameCompleted = true;
    }

    public void updateScoreTextView() {
        // If there aren't any players, don't update the score textview
        if (playerAmount == 0) {
            return;
        }

        TextView tvScoreWithAchievementLevel = findViewById(R.id.tvScoreWithAchievementLevel);

        // If we increase decrease the player amount, then increase, then decrease again (and all fields are filled)
        // The game game is still completed. Check if any fields are null and if not, update the text
        if (recyclerViewAdapter.getScores() != null){
            gameCompleted = true;
        }

        if (!gameCompleted) {
            tvScoreWithAchievementLevel.setText(R.string.waiting_player_score_input);
            return;
        }

        String achievementTitle = gameType.getAchievementLevel(totalScore, playerAmount, difficulty);
        tvScoreWithAchievementLevel.setText(getString(R.string.display_score, totalScore, achievementTitle));
    }

}