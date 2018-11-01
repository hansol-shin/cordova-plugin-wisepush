#import <UIKit/UIKit.h>
#import <Cordova/CDVPlugin.h>

@interface WPPlugin : CDVPlugin
{
    //NSString *notificationCallBack;
}

+ (WPPlugin *) wpPlugin;
- (void)ready:(CDVInvokedUrlCommand*)command;
- (void)getToken:(CDVInvokedUrlCommand*)command;
- (void)subscribeToTopic:(CDVInvokedUrlCommand*)command;
- (void)unsubscribeFromTopic:(CDVInvokedUrlCommand*)command;
- (void)registerNotification:(CDVInvokedUrlCommand*)command;
- (void)notifyOfMessage:(NSData*) payload;
- (void)notifyOfTokenRefresh:(NSString*) token;
- (void)appEnterBackground;
- (void)appEnterForeground;
- (void)getDeviceId:(CDVInvokedUrlCommand*)command;
- (void)getClientId:(CDVInvokedUrlCommand*)command;


@end
