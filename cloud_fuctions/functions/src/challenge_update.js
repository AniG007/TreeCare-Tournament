//Update path variables
process.env.GCLOUD_PROJECT = 'smooth-pivot-242815'
process.env.FIREBASE_CONFIG = {
	databaseURL: 'https://smooth-pivot-242815.firebaseio.com',
	storageBucket: 'smooth-pivot-242815.appspot.com',
	projectId: 'projectId'
}

//Function code starts

const functions = require('firebase-functions')
const admin = require('firebase-admin')

admin.initializeApp({
	apiKey: '' //API Key, 
	authDomain: 'smooth-pivot-242815.firebaseapp.com',
	projectId: 'smooth-pivot-242815',
	databaseURL: "https://smooth-pivot-242815.firebaseio.com"
})

const db = admin.firestore()

updateChallengeDaily = functions.https.onRequest((req, res) => {

	db.collection('challenges').get().then((querySnapshot) => {
		querySnapshot.forEach((doc) => {
			console.log(`${doc.id} => ${doc.data()}`);

			const challenge = doc.data();

			for (let property in challenge) {
				if (challenge.hasOwnProperty(property)) {
					console.log(challenge[property]);
				}
			}

			const endTime = challenge.finishTimestamp.toDate().getTime();
			const currentTime = new Date().getTime()
			
			if (endTime < currentTime ) {
				db.collection('challenges').doc(challenge.name)
					.set({
						active: false
					}, {merge: true})
			}
		})
	})
})

updateChallengeDaily()
