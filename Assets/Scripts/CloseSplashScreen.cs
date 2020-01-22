using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.SceneManagement;

public class CloseSplashScreen : MonoBehaviour
{
    // Start is called before the first frame update
    void Start()
    {
        StartCoroutine(SwitchSplashScreen());
    }
    
    IEnumerator SwitchSplashScreen()
    {
        yield return new WaitForSeconds(5);
        SceneManager.LoadScene("menu");
    }
}
