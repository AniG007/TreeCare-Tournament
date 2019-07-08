package dal.mitacsgri.treecare.screens.dialog.challengecomplete

import android.text.SpannedString
import androidx.annotation.ColorInt
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.lifecycle.ViewModel

class ChallengeCompleteDialogViewModel: ViewModel() {

    fun getChallengerPositionText(@ColorInt color: Int, position: Int): SpannedString {
        return buildSpannedString {
            append("You finished at position ")
            color(color) {
                append("#$position")
            }
        }
    }

}