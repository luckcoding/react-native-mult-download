//
//  RNDownload.m
//  react_native_download
//
//  Created by 朱盼 on 2018/10/23.
//  Copyright © 2018年 Facebook. All rights reserved.
//

#import "RNDownload.h"
#import <UIKit/UIKit.h>

@implementation RNDownload

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(getDeviceName:(RCTResponseSenderBlock)callback) {
  @try{
    NSString *deviceName = [[UIDevice currentDevice] name];
    callback(@[[NSNull null], deviceName]);
  }
  @catch(NSException *exception){
    callback(@[exception.reason, [NSNull null]]);
  }
}

@end
