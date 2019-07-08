package dal.mitacsgri.treecare

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import dal.mitacsgri.treecare.consts.COLLECTION_USERS
import dal.mitacsgri.treecare.extensions.toJson
import dal.mitacsgri.treecare.model.User

class TestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        Firebase.firestore.collection(COLLECTION_USERS).document("BwqCzUvuDdbCz17H8t0VAmnmOG3").get()
            .addOnSuccessListener {
                getSharedPreferences("test", Context.MODE_PRIVATE)
                    .edit {
                        putString("User", it.toObject<User>()?.toJson<User>())
                    }
                Firebase.firestore.collection(COLLECTION_USERS).document("KBwqCzUvuDdbCz17H8t0VAmnmOG3")
                    .set(
                        Gson().fromJson(getSharedPreferences("test", Context.MODE_PRIVATE).getString("User", ""),
                            User::class.java))
            }


    }
}
