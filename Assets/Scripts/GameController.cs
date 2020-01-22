using System.Collections;
using TMPro;
using UnityEngine;
using UnityEngine.UI;
using static PlayerPrefsConsts;

public class GameController : MonoBehaviour {

    public TextMeshProUGUI textDailyStepsTaken;
    public TextMeshProUGUI textDailyGoal;
    public TextMeshProUGUI textLeavesCount;
    public TextMeshProUGUI textFruitsCount;
    public TextMeshProUGUI textLeaderboardPosition;

    public Button settingsButton;
    public Button backButton;
    public Button helpButton;
    public Button progressReportButton;
    public Button leaderboardButton;

    public GameObject panelFruitCount;
    public GameObject panelLeaderboard;

    public GameObject hero;

    public GameObject cloud;
    public GameObject cloud1;
    public GameObject cloud2;
    public GameObject cloud3;
    public GameObject cloud4;
    public GameObject cloud5;

    public int cloudDelaySec;

    private GameObject[] leaves;
    private GameObject[] fruits;

    private readonly Vector2[] cloudsStartPositions = new Vector2[3];
    private GameObject[] cloudPrefabs;

    void Start() {
        Debug.Log("Starting");
        leaves = GameObject.FindGameObjectsWithTag("Leaf");
        fruits = GameObject.FindGameObjectsWithTag("Fruit");

        panelLeaderboard.SetActive(false);

        HideAllTreeItems(leaves);
        HideAllTreeItems(fruits);

        UpdateTreeLeaves();

        settingsButton.onClick.AddListener(OpenSettings);
        helpButton.onClick.AddListener(OpenHelp);
        progressReportButton.onClick.AddListener(OpenProgressReport);
        leaderboardButton.onClick.AddListener(OpenLeaderboard);

        var mode = PlayerPrefs.GetInt(GAME_MODE);
        if (mode == STARTER_MODE) {
            textDailyGoal.text = "/ " + PlayerPrefs.GetInt(DAILY_STEPS_GOAL);
            UpdateDailyStepsText(PlayerPrefs.GetInt(DAILY_STEP_COUNT),
                PlayerPrefs.GetInt(DAILY_STEPS_GOAL));

            UpdateTreeFruits();
        }
        else if(mode == CHALLENGER_MODE) {
            UpdateUiForChallengerMode();
        }

        backButton.onClick.AddListener(GoBack);

        StartCoroutine(UpdateStepCountPeriodically());
        InitializeClouds();
    }

    //Support Android back button
    void Update() {
        if (Input.GetKeyDown(KeyCode.Escape)) {
            Application.Quit();
        }

        if (PlayerPrefs.GetInt(GAME_MODE) == CHALLENGER_MODE) {
            textLeaderboardPosition.text = PlayerPrefs.GetInt(LEADERBOARD_POSITION).ToString();
        }
    }

    private IEnumerator UpdateStepCountPeriodically() {
        while (true) {
            var gameMode = PlayerPrefs.GetInt(GAME_MODE);
                UpdateDailyStepsText(PlayerPrefs.GetInt(DAILY_STEP_COUNT),
                    PlayerPrefs.GetInt(DAILY_STEPS_GOAL));
                Debug.Log("Coroutine running");
            yield return new WaitForSeconds(5);
        }
    }

    private void HideAllTreeItems(GameObject[] items) {
        foreach (var item in items) {
            item.SetActive(false);
        }
    }

    private void UpdateTreeLeaves() {
        if (PlayerPrefs.GetInt(GAME_MODE) == STARTER_MODE) {

            var lastLeafCount = PlayerPrefs.GetInt(LAST_LEAF_COUNT, 0);
            var currentLeafCount = PlayerPrefs.GetInt(CURRENT_LEAF_COUNT);

            var leafDiff = currentLeafCount - lastLeafCount;

            //The default value needs to be true for the first run to prevent execution of this code the first time
            if (leafDiff < 0 && PlayerPrefsX.GetBool(DAILY_GOAL_CHECKED, true)) {
                RemoveItemFromTree(leaves, leafDiff, currentLeafCount);
                //PlayerPrefsX.SetBool(DAILY_GOAL_CHECKED, true);
            }
            else {
                hero.GetComponent<HeroAutoMove>().RemoveHeroGameObject();
                for (var i = 0; i < currentLeafCount; i++) {
                    leaves[i].SetActive(true);
                }
            }

            textLeavesCount.text = currentLeafCount.ToString();
        }

        else if (PlayerPrefs.GetInt(GAME_MODE) == CHALLENGER_MODE) {
            var lastLeafCount = PlayerPrefs.GetInt(CHALLENGE_LAST_LEAF_COUNT, 0);
            var currentLeafCount = PlayerPrefs.GetInt(CHALLENGE_LEAF_COUNT);

            var leafDiff = currentLeafCount - lastLeafCount;

            //The default value needs to be true for the first run to prevent execution of this code the first time
            //if (leafDiff < 0 && PlayerPrefsX.GetBool(DAILY_GOAL_CHECKED, true)) {
            //    RemoveItemFromTree(leaves, leafDiff, currentLeafCount);
            //    //PlayerPrefsX.SetBool(DAILY_GOAL_CHECKED, true);
            //}
            //else {
                hero.GetComponent<HeroAutoMove>().RemoveHeroGameObject();
                for (var i = 0; i < currentLeafCount; i++) {
                    leaves[i].SetActive(true);
                }
            //}
        }
    }

    private void UpdateTreeFruits() {
        bool dailyGoalChecked = PlayerPrefsX.GetBool(DAILY_GOAL_CHECKED, false);
        int currentFruitCount = PlayerPrefs.GetInt(CURRENT_FRUIT_COUNT, 0);

        //Daily goal should be unchecked and it should not be the first run
        //It means this code will run from the second day, because then we will have data for previous day
        //The code in Android part ensures that it is never executed on the first run
        //It will be executed but will have no effect upfront
        if (!dailyGoalChecked) {

            Debug.Log("Executing algorithm");
            int lastFruitCount = PlayerPrefs.GetInt(LAST_FRUIT_COUNT, 0);

            if (currentFruitCount < lastFruitCount) {
                RemoveItemFromTree(fruits, lastFruitCount - currentFruitCount, 
                    currentFruitCount);
            } else {
                for (int i = 0; i < currentFruitCount; i++) {
                    fruits[i].SetActive(true);
                }
            }
            PlayerPrefsX.SetBool(DAILY_GOAL_CHECKED, true);
        }
        else {
            for (int i = 0; i < currentFruitCount; i++) {
                fruits[i].SetActive(true);
            }
        }

        textFruitsCount.text = currentFruitCount.ToString();
        Debug.Log(textFruitsCount.text);
    }

    private void RemoveItemFromTree(GameObject[] items, int itemCountToRemove, int totalItems) {

        for (int i = 0; i < totalItems; i++) {
            items[i].SetActive(true);
        }

        for (int i = totalItems; i >= totalItems - itemCountToRemove; i--)
        {
            var item = items[i];
            item.GetComponent<Rigidbody2D>().simulated = true;
            Destroy(item.GetComponent<HingeJoint2D>());
        }

        //Start the animation for hero to collect items
        hero.GetComponent<HeroAutoMove>().StartRunAnimation(Time.time);
    }

    private void UpdateDailyStepsText(int stepsTaken, int dailyGoal = 0) {
        textDailyStepsTaken.text = stepsTaken.ToString();

        var halfGoalCheckpoint = Mathf.CeilToInt(dailyGoal / 2f);

        Color goalFirstHalfTop = new Color32(0xff, 0x00, 0x00, 0xff);
        Color goalFirstHalfBottom = new Color32(0x84, 0x00, 0x00, 0xff);
        Color goalSecondHalfTop = new Color32(0xff, 0xe6, 0x00, 0xff);
        Color goalSecondHalfBottom = new Color32(0x9C, 0x8D, 0x00, 0xFF);
        Color goalAchievedTop = new Color32(0x00, 0xff, 0x00, 0xff);
        Color goalAchievedBottom = new Color32(0x03, 0x93, 0x00, 0xFF);

        textDailyStepsTaken.enableVertexGradient = true;
        if (stepsTaken < halfGoalCheckpoint) {
            textDailyStepsTaken.colorGradient = new VertexGradient(
                goalFirstHalfTop, goalFirstHalfTop, goalFirstHalfBottom, goalFirstHalfBottom);
        }
        else if (stepsTaken >= halfGoalCheckpoint && stepsTaken < dailyGoal) {
            textDailyStepsTaken.colorGradient = new VertexGradient(
                goalSecondHalfTop, goalSecondHalfTop, goalSecondHalfBottom, goalSecondHalfBottom);
        }
        else {
            textDailyStepsTaken.colorGradient = new VertexGradient(
                goalAchievedTop, goalAchievedTop, goalAchievedBottom, goalAchievedBottom);
        }
    }

    private void UpdateUiForChallengerMode() {
        var type = PlayerPrefs.GetInt(CHALLENGE_TYPE);
        var currentLeafCount = PlayerPrefs.GetInt(CHALLENGE_LEAF_COUNT);

        UpdateDailyStepsText(PlayerPrefs.GetInt(DAILY_STEP_COUNT), 
            PlayerPrefs.GetInt(CHALLENGE_GOAL));
            //else if (type == CHALLENGE_TYPE_AGGREGATE_BASED) {
        //    UpdateDailyStepsText(PlayerPrefs.GetInt(CHALLENGE_TOTAL_STEPS_COUNT),
        //        PlayerPrefs.GetInt(CHALLENGE_GOAL));
        //}

        textLeavesCount.text = currentLeafCount.ToString();
        
        //Adding daily goal type here, earlier it was aggregate type
        if (type == CHALLENGE_TYPE_DAILY_GOAL_BASED) {
            textDailyGoal.text = "/ " + PlayerPrefs.GetInt(CHALLENGE_GOAL);
        }

        panelFruitCount.SetActive(false);
        panelLeaderboard.SetActive(true);
    }

    private void OpenSettings() {
        //SceneManager.LoadScene("menu");
        CallAndroidMethod("OpenSettings");
    }

    private void OpenHelp() {
        CallAndroidMethod("OpenHelp");
    }

    private void OpenProgressReport() {
        CallAndroidMethod("OpenProgressReport");
    }

    private void OpenLeaderboard() {
        CallAndroidMethod("OpenLeaderboard");
    }

    private void GoBack() {
        Application.Quit();
    }

    private void CallAndroidMethod(string methodName) {
        if (Application.platform == RuntimePlatform.Android) {
            AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject jo = jc.GetStatic<AndroidJavaObject>("currentActivity");
            jo.Call(methodName);
        }
    }

    private void InitializeClouds() {
        cloudsStartPositions[0] = new Vector3(6, 3.88f);
        cloudsStartPositions[1] = new Vector3(6, 1.92f);
        cloudsStartPositions[2] = new Vector3(6, 2.86f);

        cloudPrefabs = new GameObject[6];
        cloudPrefabs[0] = cloud;
        cloudPrefabs[1] = cloud1;
        cloudPrefabs[2] = cloud2;
        cloudPrefabs[3] = cloud3;
        cloudPrefabs[4] = cloud4;
        cloudPrefabs[5] = cloud5;

        StartCoroutine(InstantiateCloud());
    }

    private IEnumerator InstantiateCloud() {
        while (true) {
            Vector2 position = new Vector3(6, Random.Range(1.9f, 3.9f));
            GameObject cloudPrefab = cloudPrefabs[Random.Range(0, 5)];
            Instantiate(cloudPrefab, position, Quaternion.identity);
            Debug.Log("Cloud created");
            yield return new WaitForSeconds(cloudDelaySec);
        }
    }
}
