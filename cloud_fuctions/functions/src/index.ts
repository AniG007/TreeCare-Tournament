import * as functions from 'firebase-functions';

 //Start writing Firebase Functions
 // https://firebase.google.com/docs/functions/typescript

export var modifyUser = functions.firestore
	.document('users/{userID}')
	.onWrite((change, context) => {
		// Get an object with the current document value.
		// If the document does not exist, it has been deleted.
		const document = change.after.exists ? change.after.data() : null;
		const oldDocument = change.before.data();

		// Get an object with the previous document value (for update or delete)
		
	});