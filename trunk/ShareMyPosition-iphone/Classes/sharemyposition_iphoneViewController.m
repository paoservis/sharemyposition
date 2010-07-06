//
//  sharemyposition_iphoneViewController.m
//  sharemyposition-iphone
//
//  Created by Sylvain Maucourt on 28/06/10.
//  Copyright Deveryware 2010. All rights reserved.
//

#import "sharemyposition_iphoneViewController.h"

@implementation sharemyposition_iphoneViewController


@synthesize lastLocation;

/*
// The designated initializer. Override to perform setup that is required before the view is loaded.
- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    if ((self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil])) {
        // Custom initialization
    }
    return self;
}
*/

/*
// Implement loadView to create a view hierarchy programmatically, without using a nib.
- (void)loadView {
}
*/


// Implement viewDidLoad to do additional setup after loading the view, typically from a nib.
- (void)viewDidLoad {
	locationController = [[MyCLController alloc] init];
	locationController.delegate = self;
    [super viewDidLoad];
	[self locateMeNow:self];
}


/*
// Override to allow orientations other than the default portrait orientation.
- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
    // Return YES for supported orientations
    return (interfaceOrientation == UIInterfaceOrientationPortrait);
}
*/

- (void)didReceiveMemoryWarning {
	// Releases the view if it doesn't have a superview.
    [super didReceiveMemoryWarning];
	
	// Release any cached data, images, etc that aren't in use.
}

- (void)viewDidUnload {
	// Release any retained subviews of the main view.
	// e.g. self.myOutlet = nil;
}


- (void)dealloc {
    [locationController release];
	[lastLocation release];
    [super dealloc];
}

-(IBAction)locateMeNow:(id)sender {
	NSLog(@"locate me now ...");
	[activity startAnimating];
    [locationController.locationManager startUpdatingLocation];
}

- (void)locationUpdate:(CLLocation*)location {
	NSLog(@"update location with %@", [location description]);
	
	[activity stopAnimating];
	
	NSTimeInterval locationAge = -[location.timestamp timeIntervalSinceNow];
    if (locationAge > 5.0) {
		NSLog(@"you will not want to rely on cached measurements");
		return;
	}
	
	if (lastLocation == nil || lastLocation.horizontalAccuracy > location.horizontalAccuracy) {
		self.lastLocation = location;
	} else {
		NSLog(@"nothing to do because lastLocation is more accurate.");
		return;
	}
	
	NSURL *url = [NSURL URLWithString:
				   [NSString stringWithFormat:@"http://sharemyposition.appspot.com/sharedmap.jsp?pos=%f,%f&size=320x220",
						location.coordinate.latitude,
						location.coordinate.longitude
				   ]
				  ];
	NSLog(@"loading url .. %@", url);
	
	[preview loadRequest:[NSURLRequest requestWithURL:url]];
	geocodeAddressSwitch.enabled=YES;
	shareBySMS.enabled=YES;
}

- (void)locationError:(NSError*)error {
	NSLog(@"error %@", [error description]);
	[activity stopAnimating];
}

- (IBAction)shareItBySMS:(id)sender {
	NSLog(@"share by SMS selected");
	NSLog(@"treating lastLocation ... %@", [self.lastLocation description]);
	
	[self stopRequestLocation];
}

- (void)stopRequestLocation {
	NSLog(@"stopping request location");
    [locationController.locationManager stopUpdatingLocation];
}

- (NSString*)shorteningUrl:(NSString*)url {
	return @"";
}

@end
