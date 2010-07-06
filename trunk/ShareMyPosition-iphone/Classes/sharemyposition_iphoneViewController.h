//
//  sharemyposition_iphoneViewController.h
//  sharemyposition-iphone
//
//  Created by Sylvain Maucourt on 28/06/10.
//  Copyright Deveryware 2010. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <CoreLocation/CoreLocation.h>
#import "MyCLController.h"

@interface sharemyposition_iphoneViewController : UIViewController<MyCLControllerDelegate> {
	
	MyCLController *locationController;
	IBOutlet UIWebView *preview;
	IBOutlet UIBarButtonItem *locateMe;
	IBOutlet UIActivityIndicatorView *activity;
	IBOutlet UIButton *shareBySMS;
	IBOutlet UIButton *shareByMAIL;
	IBOutlet UIButton *shareByGoogleLatitude;
	IBOutlet UISwitch *geocodeAddressSwitch;
	
	CLLocation *lastLocation;
}

@property (nonatomic, retain) CLLocation *lastLocation;

- (IBAction)locateMeNow:(id)sender;
- (IBAction)shareItBySMS:(id)sender;
- (NSString*)shorteningUrl:(NSString*)url;
- (void)locationUpdate:(CLLocation*)location;
- (void)locationError:(NSError*)error;
- (void)stopRequestLocation;
- (NSString*)shareIt;

@end

