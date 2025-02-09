/**TEAM PATH TO HEAVEN
 * Authors:
 *  - Hasitha Dias:   789929
 *  - Jay Parikh:     864675
 *  - Anupama Sodhi:  791288
 *  - Kushagra Gupta: 804729
 *  - Manindra Arora: 827703
 * **/

//This class is used to restart the GPS Location Service(SensorService) if it ever stopped.//

package com.example.jay1805.itproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SensorRestarterBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, SensorService.class));;
    }
}
