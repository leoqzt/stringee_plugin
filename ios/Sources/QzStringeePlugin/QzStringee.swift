import Foundation

@objc public class QzStringee: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}
