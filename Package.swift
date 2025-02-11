// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "Qzstringee",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "Qzstringee",
            targets: ["QzStringeePlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", branch: "main")
    ],
    targets: [
        .target(
            name: "QzStringeePlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/QzStringeePlugin"),
        .testTarget(
            name: "QzStringeePluginTests",
            dependencies: ["QzStringeePlugin"],
            path: "ios/Tests/QzStringeePluginTests")
    ]
)