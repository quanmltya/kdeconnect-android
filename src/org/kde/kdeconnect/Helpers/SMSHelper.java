/*
 * Copyright 2018 Simon Redman <simon@ergotech.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License or (at your option) version 3 or any later version
 * accepted by the membership of KDE e.V. (or its successor approved
 * by the membership of KDE e.V.), which shall act as a proxy
 * defined in Section 14 of version 3 of the license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

package org.kde.kdeconnect.Helpers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SMSHelper {

    /**
     * Get the base address for the SMS content
     * <p>
     * If we want to support API < 19, it seems to be possible to read via this query
     * This is highly undocumented and very likely varies between vendors but appears to work
     */
    protected static Uri getSMSURIBad() {
        return Uri.parse("content://sms/");
    }

    /**
     * Get the base address for the SMS content
     * <p>
     * Use the new API way which should work on any phone API >= 19
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    protected static Uri getSMSURIGood() {
        return Telephony.Sms.CONTENT_URI;
    }

    protected static Uri getSMSUri() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return getSMSURIGood();
        } else {
            return getSMSURIBad();
        }
    }

    /**
     * Get the base address for all message conversations
     */
    protected static Uri getConversationUri() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return Telephony.MmsSms.CONTENT_CONVERSATIONS_URI;
        } else {
            // As with getSMSUriBad, this is potentially unsafe depending on whether a specific
            // manufacturer decided to do their own thing
            return Uri.parse("content://mms-sms/conversations");
        }
    }

    /**
     * Get all the messages in a requested thread
     *
     * @param context  android.content.Context running the request
     * @param threadID Thread to look up
     * @return List of all messages in the thread
     */
    public static List<Message> getMessagesInThread(Context context, ThreadID threadID) {
        List<Message> toReturn = new ArrayList<>();

        Uri smsUri = getSMSUri();

        final String selection = ThreadID.lookupColumn + " == ?";
        final String[] selectionArgs = new String[] { threadID.toString() };

        Cursor smsCursor = context.getContentResolver().query(
                smsUri,
                Message.smsColumns,
                selection,
                selectionArgs,
                null);

        if (smsCursor != null && smsCursor.moveToFirst()) {
            int threadColumn = smsCursor.getColumnIndexOrThrow(ThreadID.lookupColumn);
            do {
                int thread = smsCursor.getInt(threadColumn);

                Message messageInfo = new Message();
                for (int columnIdx = 0; columnIdx < smsCursor.getColumnCount(); columnIdx++) {
                    String colName = smsCursor.getColumnName(columnIdx);
                    String body = smsCursor.getString(columnIdx);
                    messageInfo.put(colName, body);
                }
                toReturn.add(messageInfo);
            } while (smsCursor.moveToNext());
        } else {
            // No SMSes available?
        }

        if (smsCursor != null) {
            smsCursor.close();
        }

        return toReturn;
    }

    /**
     * Get the last message from each conversation. Can use those thread_ids to look up more
     * messages in those conversations
     *
     * @param context android.content.Context running the request
     * @return Mapping of thread_id to the first message in each thread
     */
    public static Map<ThreadID, Message> getConversations(Context context) {
        HashMap<ThreadID, Message> toReturn = new HashMap<>();

        Uri conversationUri = getConversationUri();

        Cursor conversationsCursor = context.getContentResolver().query(
                conversationUri,
                Message.smsColumns,
                null,
                null,
                null);

        if (conversationsCursor != null && conversationsCursor.moveToFirst()) {
            int threadColumn = conversationsCursor.getColumnIndexOrThrow(ThreadID.lookupColumn);
            do {
                int thread = conversationsCursor.getInt(threadColumn);

                Message messageInfo = new Message();
                for (int columnIdx = 0; columnIdx < conversationsCursor.getColumnCount(); columnIdx++) {
                    String colName = conversationsCursor.getColumnName(columnIdx);
                    String body = conversationsCursor.getString(columnIdx);
                    messageInfo.put(colName, body);
                }
                toReturn.put(new ThreadID(thread), messageInfo);
            } while (conversationsCursor.moveToNext());
        } else {
            // No conversations available?
        }

        if (conversationsCursor != null) {
            conversationsCursor.close();
        }

        return toReturn;
    }

    /**
     * Represent an ID used to uniquely identify a message thread
     */
    public static class ThreadID {
        Integer threadID;
        static final String lookupColumn = Telephony.Sms.THREAD_ID;

        public ThreadID(Integer threadID) {
            this.threadID = threadID;
        }

        public String toString() {
            return this.threadID.toString();
        }

        @Override
        public int hashCode() {
            return this.threadID.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (other.getClass().isAssignableFrom(ThreadID.class)) {
                return ((ThreadID) other).threadID.equals(this.threadID);
            }

            return false;
        }
    }

    /**
     * Represent a message and all of its interesting data columns
     */
    public static class Message extends HashMap<String, String> {
        /**
         * Define the columns which are extracted from the Android SMS database
         */
        public static final String[] smsColumns = new String[]{
                Telephony.Sms.ADDRESS,  // Phone number of the remote
                Telephony.Sms.BODY,     // Body of the message
                Telephony.Sms.DATE,     // Some date associated with the message (Received?)
                Telephony.Sms.TYPE,     // Compare with Telephony.TextBasedSmsColumns.MESSAGE_TYPE_*
                Telephony.Sms.PERSON,   // Some obscure value that corresponds to the contact
                Telephony.Sms.READ,     // Whether we have received a read report for this message (int)
                ThreadID.lookupColumn, // Magic number which binds (message) threads
        };

        @Override
        public String toString() {
            if (this.containsKey(Telephony.Sms.BODY)) {
                return this.get(Telephony.Sms.BODY);
            }

            return super.toString();
        }
    }
}

