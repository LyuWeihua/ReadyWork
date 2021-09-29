
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for demo
-- ----------------------------
DROP TABLE IF EXISTS `demo`;
CREATE TABLE `demo` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(20) DEFAULT NULL,
  `gender` int DEFAULT '1',
  `age` int DEFAULT NULL,
  `height` int DEFAULT NULL,
  `weight` int DEFAULT NULL,
  `hobbies` text,
  `created` datetime DEFAULT NULL,
  `modified` datetime DEFAULT NULL,
  `status` int DEFAULT NULL,
  `isDeleted` bit(1) DEFAULT NULL,
  `version` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- ----------------------------
-- Records of demo
-- ----------------------------
BEGIN;
INSERT INTO `demo` VALUES (1, 'Jimmy', 1, 18, 170, 65, 'Study', '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (2, 'name2', 0, 18, 170, 68, 'PC Games', '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (3, 'name3', 1, 20, 168, 65, NULL, '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (4, 'name4', 0, 20, 172, 70, NULL, '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (5, 'name5', 1, 22, 170, 72, NULL, '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (6, 'name6', 0, 23, 165, 60, NULL, '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (7, 'name7', 1, 25, 175, 80, NULL, '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (8, 'name8', 0, 25, 172, 75, NULL, '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (9, 'name9', 1, 26, 160, 60, NULL, '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (10, 'name10', 0, 28, 165, 68, NULL, '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (11, 'name11', 1, 28, 168, 65, NULL, '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (12, 'name12', 1, 28, 172, 72, NULL, '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (13, 'name13', 0, 30, 175, 75, NULL, '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (14, 'name14', 1, 30, 168, 80, 'Sports', '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (15, 'name15', 1, 32, 165, 60, 'Study', '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (16, 'name16', 0, 33, 178, 75, NULL, '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (17, 'name17', 1, 33, 180, 80, NULL, '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (18, 'name18', 1, 20, 175, 72, NULL, '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (19, 'name19', 0, 36, 168, 60, NULL, '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
INSERT INTO `demo` VALUES (20, 'name20', 1, 36, 172, 65, 'Chess', '2021-08-08 08:08:08', '2021-08-08 08:08:08', 1, b'0', 0);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
