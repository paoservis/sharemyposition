//
//  sharemyposition_iphoneAppDelegate.h
//  sharemyposition-iphone
//
//  Created by Sylvain Maucourt on 28/06/10.
//  Copyright Deveryware 2010. All rights reserved.
//

#import <UIKit/UIKit.h>

@class sharemyposition_iphoneViewController;

@interface sharemyposition_iphoneAppDelegate : NSObject <UIApplicationDelegate> {
    UIWindow *window;
    sharemyposition_iphoneViewController *viewController;
}

@property (nonatomic, retain) IBOutlet UIWindow *window;
@property (nonatomic, retain) IBOutlet sharemyposition_iphoneViewController *viewController;

@end

