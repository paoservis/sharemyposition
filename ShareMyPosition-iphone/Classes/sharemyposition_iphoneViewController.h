//
//  sharemyposition_iphoneViewController.h
//  sharemyposition-iphone
//
//  Created by Sylvain Maucourt on 28/06/10.
//  Copyright Deveryware 2010. All rights reserved.
//

#import <UIKit/UIKit.h>
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
}

- (IBAction)locateMeNow:(id)sender;
- (void)locationUpdate:(CLLocation*)location;
- (void)locationError:(NSError*)error;

@end

