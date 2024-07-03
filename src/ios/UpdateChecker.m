#import <Cordova/CDV.h>

@interface UpdateChecker : CDVPlugin

@property(nonatomic, assign) long lastModified;
@property(nonatomic, strong) NSString *launchUrl;
@property(nonatomic, strong) NSString *reloadCallbackId;

- (void)pluginInitialize;
- (void)checkForUpdate;
- (void)showUpdateDialog;
- (void)reloadWebView;

@end

@implementation UpdateChecker

- (void)pluginInitialize {
  NSLog(@"Plugin initialized");
  self.lastModified = 0;
  self.launchUrl = [self getLaunchUrlFromConfig];
  if (self.launchUrl != nil && [self.launchUrl length] > 0) {
    NSLog(@"Update check URL set to: %@", self.launchUrl);
    [self checkForUpdate];
  } else {
    NSLog(@"No URL set for update checking");
  }
}

- (void)onResume:(NSNotification *)notification {
  NSLog(@"App resumed, starting update checker");
  [self checkForUpdate];
}

- (NSString *)getLaunchUrlFromConfig {
  NSString *launchUrl = nil;
  NSString *configPath = [[NSBundle mainBundle] pathForResource:@"config"
                                                         ofType:@"xml"];
  NSData *configData = [NSData dataWithContentsOfFile:configPath];
  NSXMLParser *parser = [[NSXMLParser alloc] initWithData:configData];
  [parser setDelegate:self];
  if ([parser parse]) {
    launchUrl = self.launchUrl;
  }
  return launchUrl;
}

- (void)checkForUpdate {
  if (self.launchUrl == nil || [self.launchUrl length] == 0) {
    NSLog(@"No URL set for update checking");
    return;
  }

  NSURL *url = [NSURL URLWithString:self.launchUrl];
  NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:url];
  [request setHTTPMethod:@"HEAD"];

  NSURLSession *session = [NSURLSession sharedSession];
  [[session
      dataTaskWithRequest:request
        completionHandler:^(NSData *data, NSURLResponse *response,
                            NSError *error) {
          if (error) {
            NSLog(@"Error checking for updates: %@",
                  error.localizedDescription);
            return;
          }

          NSHTTPURLResponse *httpResponse = (NSHTTPURLResponse *)response;
          NSString *lastModifiedString =
              [httpResponse allHeaderFields][@"Last-Modified"];
          NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
          [dateFormatter setDateFormat:@"EEE, dd MMM yyyy HH:mm:ss zzz"];
          NSDate *lastModifiedDate =
              [dateFormatter dateFromString:lastModifiedString];
          self.lastModified = [lastModifiedDate timeIntervalSince1970];
          NSLog(@"Last modified time from server: %ld", self.lastModified);

          NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
          long storedTimestamp =
              [[defaults stringForKey:@"lastModified"] longLongValue];
          NSLog(@"Stored last modified time: %ld", storedTimestamp);

          if (storedTimestamp == 0) {
            NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
            [defaults
                setObject:[NSString stringWithFormat:@"%ld", self.lastModified]
                   forKey:@"lastModified"];
            [defaults synchronize];

          } else if (self.lastModified > storedTimestamp) {
            dispatch_async(dispatch_get_main_queue(), ^{
              NSLog(@"Update available, prompting user to reload");
              [self showUpdateDialog];
            });
          } else {
            NSLog(@"No update available");
          }
        }] resume];
}

- (void)showUpdateDialog {
  UIAlertController *alertController = [UIAlertController
      alertControllerWithTitle:@"Update Available"
                       message:@"A new version of the application is "
                               @"available. Please update now"
                preferredStyle:UIAlertControllerStyleAlert];
  UIAlertAction *reloadAction = [UIAlertAction
      actionWithTitle:@"Update"
                style:UIAlertActionStyleDefault
              handler:^(UIAlertAction *action) {
                NSUserDefaults *defaults =
                    [NSUserDefaults standardUserDefaults];
                [defaults
                    setObject:[NSString
                                  stringWithFormat:@"%ld", self.lastModified]
                       forKey:@"lastModified"];
                [defaults synchronize];
                [self reloadWebView];
              }];
  [alertController addAction:reloadAction];
  [self.viewController presentViewController:alertController
                                    animated:YES
                                  completion:nil];
}

- (void)reloadWebView {
  [(UIWebView *)self.webViewEngine.engineWebView
      loadRequest:[NSURLRequest
                      requestWithURL:[NSURL URLWithString:self.launchUrl]]];
}

#pragma mark - NSXMLParserDelegate

- (void)parser:(NSXMLParser *)parser
    didStartElement:(NSString *)elementName
       namespaceURI:(NSString *)namespaceURI
      qualifiedName:(NSString *)qualifiedName
         attributes:(NSDictionary<NSString *, NSString *> *)attributeDict {
  if ([elementName isEqualToString:@"content"]) {
    self.launchUrl = attributeDict[@"src"];
  }
}

@end
