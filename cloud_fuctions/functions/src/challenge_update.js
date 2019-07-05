const admin = require('firebase-admin')

admin.initializeApp({
	apiKey: 'AIzaSyA9ZSw2R96P-Tqe5lHS93Ew0C08EAMbREE', 
	authDomain: 'smooth-pivot-242815.firebaseapp.com',
	projectId: 'smooth-pivot-242815',
	databaseURL: "https://smooth-pivot-242815.firebaseio.com"
})

const db = admin.firestore()

db.collection('challenges').get().then((querySnapshot) => {
	querySnapshot.forEach((doc) => {
		console.log(`${doc.id} => ${doc.data()}`);
	})
})