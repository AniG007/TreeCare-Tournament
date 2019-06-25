import * as functions from 'firebase-functions';

 //Start writing Firebase Functions
 // https://firebase.google.com/docs/functions/typescript

export var modifyUser = functions.firestore
	.document('users/{userID}')
	.onWrite((change, context) => {
		if (change.after.exists) {
			// Get an object with the current document value.
			// If the document does not exist, it has been deleted.
			const document: string = change.after.data().uid;
			// Get an object with the previous document value (for update or delete)
			//const oldDocument = change.before.data();

			//let dailyGoalMap: Map<string, number> = document["dailyGoalMap"] as Map<string, number>;
			console.log(document);
		}
	});