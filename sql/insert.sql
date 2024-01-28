INSERT INTO PERMISSION (permissionName, permissionDesc, permissionLevel) VALUES ('break_block', 'casser des blocks', 2);
INSERT INTO PERMISSION (permissionName, permissionDesc, permissionLevel) VALUES ('place_block', 'placer des blocks', 2);
INSERT INTO PERMISSION (permissionName, permissionDesc, permissionLevel) VALUES ('manage_permissions', 'modifier les permissions des grades inférieurs au sien', 3);
INSERT INTO PERMISSION (permissionName, permissionDesc, permissionLevel) VALUES ('manage_settings', 'gérer les paramèttres d\'ile', 4);
INSERT INTO PERMISSION (permissionName, permissionDesc, permissionLevel) VALUES ('invite_player', 'inviter un joueur sur l\'ile', 3);
INSERT INTO PERMISSION (permissionName, permissionDesc, permissionLevel) VALUES ('kick_player', '', 3);
INSERT INTO PERMISSION (permissionName, permissionDesc, permissionLevel) VALUES ('place_shop', 'placer un magasin', 2);
INSERT INTO PERMISSION (permissionName, permissionDesc, permissionLevel) VALUES ('bank_withdraw', 'retirer de l\'argent de la banque', 4);
INSERT INTO PERMISSION (permissionName, permissionDesc, permissionLevel) VALUES ('bank_deposit', 'déposer de l\'argent dans la banque', 2);
INSERT INTO PERMISSION (permissionName, permissionDesc, permissionLevel) VALUES ('promote_player', 'élever le grade d\'un joueur de plus bas grade que sois', 3);
INSERT INTO PERMISSION (permissionName, permissionDesc, permissionLevel) VALUES ('remote_player', 'descendre le grade d\'un joueur de plus bas grade que sois', 4);
INSERT INTO PERMISSION (permissionName, permissionDesc, permissionLevel) VALUES ('place_warps', 'ajouter des warps d\'ile', 3);
INSERT INTO PERMISSION (permissionName, permissionDesc, permissionLevel) VALUES ('remove_warps', 'retirer des warps d\'ile', 3);
INSERT INTO PERMISSION (permissionName, permissionDesc, permissionLevel) VALUES ('upgrade_prestige', 'augmenter le niveau de prestige de l\'ile', 4);
INSERT INTO PERMISSION (permissionName, permissionDesc, permissionLevel) VALUES ('upgrade_phase', 'augmenter le niveau de phase d\'ile', 4);
INSERT INTO PERMISSION (permissionName, permissionDesc, permissionLevel) VALUES ('select_prestige', 'changer le prestige d\'ile actif', 3);
INSERT INTO PERMISSION (permissionName, permissionDesc, permissionLevel) VALUES ('select_phase', 'changer la phase d\'ile active', 3);

INSERT INTO SETTING (settingTitle, settingDesc) VALUES ('allow_visitors', 'Autoriser ou non les visiteurs sur ton ile');