/**
 * Firebase Cloud Function skeleton.
 * This is NOT wired into the Android project directly, but you can
 * copy it into a Firebase Functions project.
 *
 * Goal: Increment board.taskCount whenever a new task is created.
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

exports.onTaskCreated = functions.firestore
    .document("boards/{boardId}/tasks/{taskId}")
    .onCreate(async (snap, context) => {
        const boardId = context.params.boardId;

        const boardRef = db.collection("boards").doc(boardId);
        await boardRef.update({
            taskCount: admin.firestore.FieldValue.increment(1)
        });

        console.log("Task count incremented for board:", boardId);
    });
