//
//  ShareMyPositionView.h
//  sharemyposition-iphone
//
//  Created by Sylvain Maucourt on 05/07/10.
//  Copyright 2010 Deveryware. All rights reserved.
//

#import <UIKit/UIKit.h>


@interface ShareMyPositionView : UIView {

	IBOutlet UIWebView *preview;
	IBOutlet UIBarButtonItem *locateMe;
	IBOutlet UIActivityIndicatorView *activity;
}

- (void)showPreview:(NSString*) url;

@end
