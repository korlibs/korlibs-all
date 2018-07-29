
let webpack = require('webpack');

const path = require('path');

module.exports = {
    mode: 'production',
    node: false,
    target: 'web',
    entry: path.resolve(__dirname, 'web/game.js'),
    output: {
        path: path.resolve(__dirname, 'dist'),
        filename: 'bundle.js'
    },
    resolve: {
        extensions: ['.wasm', '.mjs', '.js', '.json'],
        modules: ['web'],
    },
    plugins: [
        new webpack.IgnorePlugin(/canvas|crypto|fs|http|net|os|path|url/),
    ]
};
