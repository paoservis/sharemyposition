//
//  ShareMyPositionView.m
//  sharemyposition-iphone
//
//  Created by Sylvain Maucourt on 05/07/10.
//  Copyright 2010 Deveryware. All rights reserved.
//

#import "ShareMyPositionView.h"


@implementation ShareMyPositionView


- (id)initWithFrame:(CGRect)frame {
    if ((self = [super initWithFrame:frame])) {
        // Initialization code
    }
    return self;
}

/*
// Only override drawRect: if you perform custom drawing.
// An empty implementation adversely affects performance during animation.
- (void)drawRect:(CGRect)rect {
    // Drawing code
}
*/

- (void)dealloc {
    [super dealloc];
	[preview dealloc];
	[locateMe dealloc];
	[activity dealloc];
}


- (void)showPreview:(NSString*) url {
}

@end
