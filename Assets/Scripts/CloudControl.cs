using System;
using UnityEngine;

public class CloudControl : MonoBehaviour {

    public float speed = 0.6f;

    private Vector2 startPosition;
    private Vector2 endPosition;
    private float startTime;
    private float journeyLength;

    // Start is called before the first frame update
    void Start() {
        startTime = Time.time;

        var position = transform.position;
        var yPos = position.y;

        //Perform 2D calculations to improve performance
        startPosition = new Vector2(position.x, yPos);
        endPosition = new Vector2(-6f, yPos);
        journeyLength = Vector2.Distance(startPosition, endPosition);
    }

    // Update is called once per frame
    void Update() {
        float distCovered = (Time.time - startTime) * speed;
        float fractJourney = distCovered / journeyLength; //Fraction of journey completed
        transform.position = Vector2.Lerp(startPosition, endPosition, fractJourney);

        if (Math.Abs(transform.position.x - (-6f)) < 0.1) {
            Destroy(gameObject);
            Debug.Log("Condition true");
        }
    }
}
