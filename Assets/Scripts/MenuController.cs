using UnityEngine;
using UnityEngine.SceneManagement;
using UnityEngine.UI;
using static PlayerPrefsConsts;

public class MenuController : MonoBehaviour {

    public int dailyStepsGoal = 5000;
    public Sprite inactiveArrow;
    public Sprite activeArrow;
    public Button buttonDecreaseStepsCount;
    public Text stepsCountText;

    private void Start() {
        dailyStepsGoal = PlayerPrefs.GetInt(DAILY_STEPS_GOAL);
        if (dailyStepsGoal == 0) {
            dailyStepsGoal = 5000;
        }

        if (dailyStepsGoal > 5000) {
            buttonDecreaseStepsCount.image.sprite = activeArrow;
        } else {
            buttonDecreaseStepsCount.image.sprite = inactiveArrow;
        }

        UpdateStepsCountText(dailyStepsGoal);
    }

    //Support Android back button
    void Update() {
        if (Input.GetKeyDown(KeyCode.Escape)) {
            SceneManager.LoadScene("game");
        }
    }

    public void IncreaseStepsCount() {
        dailyStepsGoal += 1000;
        if (dailyStepsGoal > 5000) {
            buttonDecreaseStepsCount.image.sprite = activeArrow;
        }
        UpdateStepsCountText(dailyStepsGoal);
    }

    public void DecreaseStepsCount() {
        if (dailyStepsGoal > 5000) {
            dailyStepsGoal -= 1000;
            UpdateStepsCountText(dailyStepsGoal);
        }

        if (dailyStepsGoal == 5000) {
            buttonDecreaseStepsCount.image.sprite = inactiveArrow;
        }
    }

    public void LaunchGameScene() {
        SceneManager.LoadScene("game");
    }

    private void UpdateStepsCountText(int steps) {
        stepsCountText.text = steps.ToString();
        StoreStepsCountInPrefs(steps);
    }

    //Store steps count in PlayerPrefs to maintain data between game sessions
    private void StoreStepsCountInPrefs(int steps) {
        PlayerPrefs.SetInt(DAILY_STEPS_GOAL, steps);
        Debug.Log("steps count: " + PlayerPrefs.GetInt(DAILY_STEPS_GOAL));
    }


}
