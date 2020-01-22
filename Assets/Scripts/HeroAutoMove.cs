using UnityEngine;

public class HeroAutoMove : MonoBehaviour {

    public float speed = 1.0f;
    public Animator animator;  

    private float startTime;
    private float journeyLength;

    private Vector2 startPosition;
    private Vector2 endPosition;

    private bool runAnimation = false;

    // Start is called before the first frame update
    void Start() {

        //Perform 2D calculations to improve performance
        startPosition = new Vector2(transform.position.x, transform.position.y);
        
        endPosition = new Vector2(3.5f, transform.position.y);

        journeyLength = Vector2.Distance(startPosition, endPosition);
    }

    // Update is called once per frame
    void Update() {
        if (runAnimation) {
            float distCovered = (Time.time - startTime) * speed;
            float fractJourney = distCovered / journeyLength; //Fraction of journey completed
            transform.position = Vector2.Lerp(startPosition, endPosition, fractJourney);

            if (transform.position.Equals(endPosition)) {
                animator.enabled = false;
            }
        }
    }

    void OnTriggerEnter2D(Collider2D other) {
        Debug.Log("Collision");
        animator.Play("Hero_react", -1, 0f);
        Destroy(other.gameObject);
    }

    public void StartRunAnimation(float startTime) {
        this.startTime = startTime;
        journeyLength = Vector2.Distance(startPosition, endPosition);
        runAnimation = true;
        gameObject.SetActive(true);
        Debug.Log("Animation started");
        animator.Play("Hero_run", -1, 0f);
    }

    //public void StopRunAnimation() {
    //  runAnimation = false;
    //    startPosition = transform.position;
    //}

    public void RemoveHeroGameObject() {
        gameObject.SetActive(false);
    }
}