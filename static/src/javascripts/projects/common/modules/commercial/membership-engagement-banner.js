define([
        'bean',
        'qwery',
        'common/utils/config',
        'common/utils/storage',
        'common/utils/template',
        'common/modules/ui/message',
        'text!common/views/membership-message.html',
        'common/modules/commercial/commercial-features',
        'common/modules/commercial/user-features',
        'common/utils/mediator',
        'Promise',
        'common/utils/fastdom-promise',
        'common/modules/experiments/ab',
        'common/modules/experiments/tests/membership-engagement-banner-tests',
        'common/utils/$',
        'lodash/objects/defaults',
        'common/views/svgs'
    ], function (bean,
                 qwery,
                 config,
                 storage,
                 template,
                 Message,
                 messageTemplate,
                 commercialFeatures,
                 userFeatures,
                 mediator,
                 Promise,
                 fastdom,
                 ab,
                 MembershipEngagementBannerTests,
                 $,
                 defaults,
                 svgs) {

        var endpoints = {
            UK: 'https://membership.theguardian.com/uk/supporter',
            US: 'https://contribute.theguardian.com',
            AU: 'https://membership.theguardian.com/au/supporter',
            INT: 'https://membership.theguardian.com/supporter'
        };



        // change messageCode to force redisplay of the message to users who already closed it.
        // messageCode is also consumed by .../test/javascripts/spec/common/commercial/membership-engagement-banner.spec.js
        var messageCode = 'engagement-banner-2016-11-10';
        var notInTest = 'notintest';

        var defaultBannerParams = {
            minArticles: 10
        };

        var requiredParams = [
            minArticles, // how many articles should the user see before they get the engagement banner?
            messageText,
            colourStrategy, // a function to determine what css class to use for the banner's colour
            buttonCaption
        ];

        var baseParams = {
            membership: {
                buttonCaption: 'Become a Supporter',
                colourStrategy: function() {
                    var colours = ['yellow', 'purple', 'bright-blue', 'dark-blue'];
                    // Rotate through different colours on successive page views
                    return 'membership-message-' + colours[storage.local.get('gu.alreadyVisited') % colours.length];
                }
            },
            contributions: {
                buttonCaption: 'Make a Contribution',
                colourStrategy: function() { return 'contributions-message'; }
            }
        };

        var editionParams = {
            uk: {
                membership: {
                    messageText: 'For less than the price of a coffee a week, you could help secure the Guardian’s future. Support our journalism for £5 a month.'
                }
            },
            au: {
                membership: {
                    messageText: 'We need you to help support our fearless independent journalism. Become a Guardian Australia Member for just $100 a year.'
                }
            },
            us: {
                contributions: {
                    messageText: 'If you use it, if you like it, then why not pay for it? It’s only fair.'
                }
            },
            int: {
                membership: {
                    messageText: 'The Guardian’s voice is needed now more than ever. Support our journalism for just $69/€49 per year.'
                }
            }
        };


        function show(edition, message) {
            var paramsForEdition = editionParams[edition];
            

            var content = {
                campaignCode: message.campaign,
                colourClass: thisInstanceColour(),
                arrowWhiteRight: svgs('arrowWhiteRight')
            };

            var userVariant = MembershipEngagementBannerTests.find(new function(test) {
                var userVariantId = getVariantIdFor(test.id);
                if (userVariantId) { // user is in a variant for this test
                    return test.variants.find(new function(variant) { variant.id = userVariantId });
                }
            });

            bannerParamsSources = [defaultEditionBannerParams, userVariantBannerParams];

            var bannerParams = defaults({ minArticles: 10 }, bannerParamsSources);

            var campaignCode = "";

            var renderedBanner = template(messageTemplate, {
                linkHref: formatEndpointUrl(edition, message.campaign),
                messageText: bannerParams.messageText,
                campaignCode: campaignCode,
                buttonCaption: bannerParams.buttonCaption
            });

            var messageShown = new Message(
                messageCode,
                {
                    pinOnHide: false,
                    siteMessageLinkName: 'membership message',
                    siteMessageCloseBtn: 'hide',
                    siteMessageComponentName: campaignCode,
                    trackDisplay: true,
                    cssModifierClass: 'membership-prominent ' + content.colourClass
                }).show(renderedBanner);
            if (messageShown) {
                mediator.emit('membership-message:display');
            }
            mediator.emit('banner-message:complete');
            return messageShown;
        }

        function init() {
            var edition = config.page.edition;
            var message = messages[edition];
            if (message) {
                if (userHasMadeEnoughVisits(edition)) {
                    return commercialFeatures.async.canDisplayMembershipEngagementBanner.then(function (canShow) {
                        if (canShow) {
                            show(edition, message);
                        }
                    });
                }
            }
            return Promise.resolve();
        }

        function userHasMadeEnoughVisits(edition) {
            if (edition === 'INT') {
                var internationalTestVariant = getVariantIdFor('MembershipEngagementInternationalExperiment');
                if (internationalTestVariant == '1st_article')
                    return true;
            }

            return (storage.local.get('gu.alreadyVisited') || 0) >= 10;
        }

        function formatEndpointUrl(edition, campaignCode) {
            return endpoints[edition] + '?INTCMP=' + campaignCode;
        }

        function thisInstanceColour() {
            var colours = ['yellow', 'purple', 'bright-blue', 'dark-blue'];
            // Rotate through different colours on successive page views
            return colours[storage.local.get('gu.alreadyVisited') % colours.length];
        }

        function getVariantIdFor(testName) {
            return ab.testCanBeRun(testName) ? ab.getTestVariantId(testName) : undefined;
        }

        return {
            init: init,
            messageCode: messageCode
        };

    }
);
