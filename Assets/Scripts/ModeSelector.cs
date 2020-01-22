using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;

public class ModeSelector : MonoBehaviour, IPointerClickHandler {

    public Selectable dropdown;
    public Image modeSelectionImage;
    private CanvasGroup modeSelectionCanvasGroup;
    private CanvasGroup dropdownCanvasGroup;
    private Dropdown dropdownModeSelection;

    // Start is called before the first frame update
    void Start() {
        modeSelectionCanvasGroup = modeSelectionImage.GetComponent<CanvasGroup>();
        dropdownModeSelection = dropdown as Dropdown;

        if (dropdownModeSelection != null) {
            //dropdownCanvasGroup = dropdownModeSelection.GetComponent<CanvasGroup>();

            dropdownModeSelection.onValueChanged.AddListener(delegate {
                DropdownValueChangedListener(dropdownModeSelection);
            });
            //HideCanvasGroup(dropdownCanvasGroup);
        }

        HideCanvasGroup(modeSelectionCanvasGroup);
    }

    // Update is called once per frame
    void Update() {
        
    }

    public void OnPointerClick(PointerEventData eventData) {
        HideCanvasGroup(modeSelectionCanvasGroup);
        //ShowCanvasGroup(dropdownCanvasGroup);
        dropdownModeSelection.Show();
    }

    private void HideCanvasGroup(CanvasGroup canvasGroup) {
        canvasGroup.alpha = 0f;
        canvasGroup.blocksRaycasts = false;
    }

    private void ShowCanvasGroup(CanvasGroup canvasGroup) {
        canvasGroup.alpha = 1f;
        canvasGroup.blocksRaycasts = true;
    }

    private void DropdownValueChangedListener(Dropdown target) {
        Debug.Log("Selected: " + target.value);
    }
}
